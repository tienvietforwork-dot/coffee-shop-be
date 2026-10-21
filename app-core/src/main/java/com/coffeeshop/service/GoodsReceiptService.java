package com.coffeeshop.service;

import com.coffeeshop.dto.request.GoodsReceiptRequest;
import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.GoodsReceiptRepository;
import com.coffeeshop.repository.SupplierReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phiếu nhập kho (mẫu 01-VT): against a purchase order or loose. Receiving goes: đối chiếu số lượng theo phiếu giao
 * của nhà cung cấp → kiểm tra hạn dùng / nhiệt độ / bao bì → nhận phần đạt, trả lại phần không đạt → mỗi dòng nhận
 * thành một lô. The receipt is not edited afterwards; it can be cancelled while none of its lots has been used.
 */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository receiptRepository;
    private final SupplierReturnRepository returnRepository;
    private final SupplierService supplierService;
    private final SupplierDebtService debtService;
    private final SupplierPaymentService paymentService;
    private final InventoryService inventoryService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> search(Long supplierId, Long purchaseOrderId, GoodsReceiptStatus status,
                                             LocalDate from, LocalDate to) {
        var receipts = receiptRepository.search(supplierId, purchaseOrderId, status,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay());
        Map<Long, Map<Long, GoodsReceiptResponse.Settlement>> bySupplier = new HashMap<>();
        return receipts.stream().map(r -> GoodsReceiptResponse.from(r, bySupplier
                .computeIfAbsent(r.getSupplier().getId(), debtService::settlements)
                .getOrDefault(r.getId(), GoodsReceiptResponse.Settlement.NONE))).toList();
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse find(Long id) {
        GoodsReceipt receipt = get(id);
        return GoodsReceiptResponse.from(receipt, debtService.settlement(receipt));
    }

    /** Nhập lẻ, without a purchase order: the supplier is still required. */
    @Transactional
    public GoodsReceiptResponse createLoose(GoodsReceiptRequest request) {
        if (request.supplierId() == null) throw new BadRequestException("Chọn nhà cung cấp");
        Supplier supplier = supplierService.active(request.supplierId());
        GoodsReceipt receipt = open(supplier, null, request, supplier.getPaymentTermDays());
        Set<Long> seen = new HashSet<>();
        for (GoodsReceiptRequest.Line line : request.lines()) {
            if (line.materialId() == null) throw new BadRequestException("Chọn nguyên liệu cho từng dòng");
            Material material = inventoryService.purchasable(line.materialId());
            if (!seen.add(material.getId())) throw new BadRequestException("Nguyên liệu bị lặp: " + material.getName());
            MaterialPackage pkg = inventoryService.packageOf(material, line.packageId());
            addLine(receipt, material, null, pkg == null ? null : pkg.getName(), pkg == null ? null : pkg.getSize(),
                    line, line.unitPrice());
        }
        return finish(receipt, request);
    }

    /** Phiếu nhập for one delivery of a purchase order; the order's status follows what has come in. */
    @Transactional
    public GoodsReceiptResponse receiveForOrder(PurchaseOrder order, GoodsReceiptRequest request) {
        if (order.getStatus() != PurchaseOrderStatus.ORDERED && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Phiếu " + order.getCode() + " không ở trạng thái chờ nhận hàng");
        }
        if (request.supplierId() != null && !request.supplierId().equals(order.getSupplier().getId())) {
            throw new BadRequestException("Nhà cung cấp khác với phiếu đặt " + order.getCode());
        }
        Map<Long, PurchaseOrderItem> items = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));
        GoodsReceipt receipt = open(order.getSupplier(), order, request, order.getPaymentTermDays());
        if (request.vatRate() == null) receipt.setVatRate(order.getVatRate());
        Set<Long> seen = new HashSet<>();
        for (GoodsReceiptRequest.Line line : request.lines()) {
            PurchaseOrderItem item = line.itemId() == null ? null : items.get(line.itemId());
            if (item == null) throw new BadRequestException("Dòng #" + line.itemId() + " không thuộc phiếu " + order.getCode());
            if (!seen.add(item.getId())) throw new BadRequestException("Dòng bị lặp: " + item.getMaterial().getName());
            GoodsReceiptItem added = addLine(receipt, item.getMaterial(), item, item.getPackageName(), item.getPackageSize(),
                    line, line.unitPrice() != null ? line.unitPrice() : item.getUnitPrice());
            if (added.getReceivedQuantity().compareTo(item.remaining()) > 0) {
                throw new BadRequestException("'" + item.getMaterial().getName() + "' chỉ còn "
                        + plain(item.remaining()) + " " + item.getMaterial().getUnit() + " chưa nhận theo phiếu đặt");
            }
            item.setReceivedQuantity(item.getReceivedQuantity().add(added.getReceivedQuantity()));
        }
        refreshOrderStatus(order);
        return finish(receipt, request);
    }

    /**
     * Hủy phiếu nhập (nhập nhầm, sai nhà cung cấp…): only while every lot is untouched and nothing was returned.
     * The lots leave the stock, the purchase order gets the quantities back and the payable disappears.
     */
    @Transactional
    public GoodsReceiptResponse cancel(Long id, String reason) {
        GoodsReceipt receipt = get(id);
        if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED) throw new BadRequestException("Phiếu đã hủy");
        if (!returnRepository.search(null, SupplierReturnStatus.COMPLETED, receipt.getId()).isEmpty()) {
            throw new BadRequestException("Phiếu đã có phiếu trả hàng, không hủy được");
        }
        Staff staff = staffService.current();
        for (GoodsReceiptItem item : receipt.getItems()) {
            if (item.getBatch() != null) inventoryService.cancelLot(item.getBatch(), staff, "Hủy phiếu nhập " + receipt.getCode());
            PurchaseOrderItem poItem = item.getPurchaseOrderItem();
            if (poItem != null) poItem.setReceivedQuantity(poItem.getReceivedQuantity().subtract(item.getReceivedQuantity()).max(BigDecimal.ZERO));
        }
        receipt.setStatus(GoodsReceiptStatus.CANCELLED);
        receipt.setCancelReason(reason.trim());
        receipt.setCancelledAt(LocalDateTime.now());
        PurchaseOrder order = receipt.getPurchaseOrder();
        if (order != null && order.getStatus() != PurchaseOrderStatus.CANCELLED) refreshOrderStatus(order);
        return GoodsReceiptResponse.from(receipt, debtService.settlement(receipt));
    }

    GoodsReceipt get(Long id) {
        return receiptRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập: " + id));
    }

    // ---- helpers ---------------------------------------------------------

    private GoodsReceipt open(Supplier supplier, PurchaseOrder order, GoodsReceiptRequest r, int termDays) {
        if (r.invoiceDate() != null && r.invoiceDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Ngày hóa đơn ở tương lai");
        }
        return receiptRepository.save(GoodsReceipt.builder()
                .code(newCode()).supplier(supplier).purchaseOrder(order).staff(staffService.current())
                .delivererName(blankToNull(r.delivererName()))
                .invoiceSeries(blankToNull(r.invoiceSeries())).invoiceNo(blankToNull(r.invoiceNo())).invoiceDate(r.invoiceDate())
                .documentImageId(r.documentImageId())
                .discountAmount(r.discountAmount() == null ? BigDecimal.ZERO : r.discountAmount())
                .vatRate(r.vatRate())
                .shippingCost(r.shippingCost() == null ? BigDecimal.ZERO : r.shippingCost())
                .dueDate(LocalDate.now().plusDays(termDays))
                .note(blankToNull(r.note()))
                .build());
    }

    /** Counts → quantities in the material's unit; the accepted part becomes a lot. */
    private GoodsReceiptItem addLine(GoodsReceipt receipt, Material material, PurchaseOrderItem poItem,
                                     String packageName, BigDecimal packageSize, GoodsReceiptRequest.Line line,
                                     BigDecimal unitPrice) {
        BigDecimal received = toUnits(line.receivedCount(), packageSize);
        BigDecimal rejected = toUnits(line.rejectedCount() == null ? BigDecimal.ZERO : line.rejectedCount(), packageSize);
        BigDecimal document = line.documentCount() == null ? received.add(rejected) : toUnits(line.documentCount(), packageSize);
        if (received.signum() == 0 && rejected.signum() == 0) {
            throw new BadRequestException("'" + material.getName() + "': nhập số lượng thực nhận hoặc số lượng trả lại");
        }
        if (rejected.signum() > 0 && (line.rejectReason() == null || line.rejectReason().isBlank())) {
            throw new BadRequestException("'" + material.getName() + "': ghi lý do không nhận (hỏng, sai quy cách, quá hạn…)");
        }
        if (received.add(rejected).compareTo(document) > 0) {
            throw new BadRequestException("'" + material.getName() + "': thực nhận + trả lại vượt số lượng trên phiếu giao");
        }
        GoodsReceiptItem item = GoodsReceiptItem.builder().goodsReceipt(receipt).material(material).purchaseOrderItem(poItem)
                .packageName(packageName).packageSize(packageSize)
                .documentQuantity(document).receivedQuantity(received).rejectedQuantity(rejected)
                .rejectReason(rejected.signum() > 0 ? line.rejectReason().trim() : null)
                .unitPrice(unitPrice).expiryDate(line.expiryDate()).temperature(line.temperature())
                .note(blankToNull(line.note())).build();
        if (received.signum() > 0) {
            BigDecimal packageCount = packageSize == null ? null : line.receivedCount();
            item.setBatch(inventoryService.receiveLot(material, received, packageSize == null ? null : packageName,
                    packageSize, packageCount, unitPrice, line.expiryDate(), receipt, poItem, receipt.getStaff(),
                    "Phiếu nhập " + receipt.getCode()
                            + (receipt.getPurchaseOrder() == null ? "" : " · đặt hàng " + receipt.getPurchaseOrder().getCode())));
        }
        receipt.getItems().add(item);
        return item;
    }

    private GoodsReceiptResponse finish(GoodsReceipt receipt, GoodsReceiptRequest request) {
        if (receipt.getDiscountAmount().compareTo(receipt.goodsAmount()) > 0) throw new BadRequestException("Giảm giá lớn hơn tiền hàng");
        receiptRepository.flush();
        BigDecimal paid = request.paidAmount() == null ? BigDecimal.ZERO : request.paidAmount();
        if (paid.compareTo(receipt.payable()) > 0) throw new BadRequestException("Số tiền trả lớn hơn tiền cần trả nhà cung cấp");
        if (paid.signum() > 0) {
            if (request.paymentMethod() == null) throw new BadRequestException("Chọn hình thức trả tiền nhà cung cấp");
            paymentService.recordOnReceipt(receipt, paid, request.paymentMethod(), request.paymentReference());
        }
        return GoodsReceiptResponse.from(receipt, debtService.settlement(receipt));
    }

    private static void refreshOrderStatus(PurchaseOrder order) {
        boolean all = order.getItems().stream().allMatch(i -> i.remaining().signum() == 0);
        boolean any = order.getItems().stream().anyMatch(i -> i.getReceivedQuantity().signum() > 0);
        if (all) {
            order.setStatus(PurchaseOrderStatus.COMPLETED);
            if (order.getCompletedAt() == null) order.setCompletedAt(LocalDateTime.now());
        } else if (order.getStatus() == PurchaseOrderStatus.COMPLETED && order.getCloseReason() != null && any) {
            // closed short on purpose (Kết thúc): stays closed
        } else {
            order.setStatus(any ? PurchaseOrderStatus.PARTIALLY_RECEIVED : PurchaseOrderStatus.ORDERED);
            order.setCompletedAt(null);
        }
    }

    private static BigDecimal toUnits(BigDecimal count, BigDecimal packageSize) {
        return packageSize == null ? count : packageSize.multiply(count).setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** PN + yyMMdd + running number of the day: PN261020-001 */
    private String newCode() {
        String prefix = "PN" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", receiptRepository.countByCodeStartingWith(prefix) + 1);
    }
}
