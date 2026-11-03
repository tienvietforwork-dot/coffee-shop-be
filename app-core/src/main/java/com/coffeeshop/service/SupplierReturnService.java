package com.coffeeshop.service;

import com.coffeeshop.dto.request.SupplierReturnRequest;
import com.coffeeshop.dto.response.SupplierReturnResponse;
import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.GoodsReceiptItem;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.SupplierReturn;
import com.coffeeshop.entity.SupplierReturnItem;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierReturnSettlement;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.SupplierReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phiếu trả hàng nhập: phiếu tạm (DRAFT) → đã trả hàng (COMPLETED: stock leaves the lots; công nợ drops or the
 * supplier refunds) or đã hủy (CANCELLED, drafts only). A completed return is final.
 */
@Service
@RequiredArgsConstructor
public class SupplierReturnService {

    private final SupplierReturnRepository returnRepository;
    private final GoodsReceiptService receiptService;
    private final InventoryService inventoryService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<SupplierReturnResponse> search(Long supplierId, SupplierReturnStatus status, Long receiptId) {
        return returnRepository.search(supplierId, status, receiptId).stream().map(SupplierReturnResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplierReturnResponse find(Long id) {
        return SupplierReturnResponse.from(get(id));
    }

    @Transactional
    public SupplierReturnResponse create(SupplierReturnRequest request) {
        GoodsReceipt receipt = receiptService.get(request.goodsReceiptId());
        if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED) throw new BadRequestException("Phiếu nhập đã hủy");
        SupplierReturn ret = returnRepository.save(SupplierReturn.builder().code(newCode())
                .supplier(receipt.getSupplier()).goodsReceipt(receipt).staff(staffService.current())
                .reason(request.reason().trim()).vatRate(receipt.getVatRate()).build());
        apply(ret, request);
        if (request.complete()) complete(ret);
        return SupplierReturnResponse.from(ret);
    }

    @Transactional
    public SupplierReturnResponse update(Long id, SupplierReturnRequest request) {
        SupplierReturn ret = draft(id);
        if (!ret.getGoodsReceipt().getId().equals(request.goodsReceiptId())) {
            throw new BadRequestException("Không đổi phiếu nhập gốc của phiếu trả");
        }
        ret.setReason(request.reason().trim());
        apply(ret, request);
        if (request.complete()) complete(ret);
        return SupplierReturnResponse.from(ret);
    }

    /** Trả hàng: the goods leave their lots. */
    @Transactional
    public SupplierReturnResponse complete(Long id) {
        SupplierReturn ret = draft(id);
        complete(ret);
        return SupplierReturnResponse.from(ret);
    }

    @Transactional
    public SupplierReturnResponse cancel(Long id, String reason) {
        SupplierReturn ret = draft(id);
        ret.setStatus(SupplierReturnStatus.CANCELLED);
        ret.setCancelReason(reason.trim());
        return SupplierReturnResponse.from(ret);
    }

    // ---- helpers ---------------------------------------------------------

    private void apply(SupplierReturn ret, SupplierReturnRequest request) {
        if (request.settlement() == SupplierReturnSettlement.REFUND && request.refundMethod() != PaymentMethod.CASH
                && request.refundMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BadRequestException("Chọn hình thức nhà cung cấp hoàn tiền (tiền mặt / chuyển khoản)");
        }
        ret.setSettlement(request.settlement());
        ret.setRefundMethod(request.settlement() == SupplierReturnSettlement.REFUND ? request.refundMethod() : null);
        ret.setAdjustmentInvoiceNo(blankToNull(request.adjustmentInvoiceNo()));
        ret.setNote(blankToNull(request.note()));
        ret.getItems().forEach(SupplierReturnItem::softDelete);
        ret.getItems().clear();
        returnRepository.flush();
        Map<Long, GoodsReceiptItem> lines = ret.getGoodsReceipt().getItems().stream()
                .collect(Collectors.toMap(GoodsReceiptItem::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        for (SupplierReturnRequest.Line l : request.items()) {
            GoodsReceiptItem line = lines.get(l.receiptItemId());
            if (line == null) throw new BadRequestException("Dòng #" + l.receiptItemId() + " không thuộc phiếu nhập " + ret.getGoodsReceipt().getCode());
            if (!seen.add(line.getId())) throw new BadRequestException("Dòng bị lặp: " + line.getMaterial().getName());
            if (line.getBatch() == null) throw new BadRequestException("'" + line.getMaterial().getName() + "' không nhận vào kho, không có gì để trả");
            BigDecimal quantity = line.getPackageSize() == null ? l.count()
                    : line.getPackageSize().multiply(l.count()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal alreadyReturned = returnRepository.sumQuantity(line.getId(), SupplierReturnStatus.COMPLETED, ret.getId());
            BigDecimal returnable = line.getReceivedQuantity().subtract(alreadyReturned);
            if (quantity.compareTo(returnable) > 0) {
                throw new BadRequestException("'" + line.getMaterial().getName() + "' chỉ còn trả được "
                        + returnable.stripTrailingZeros().toPlainString() + " " + line.getMaterial().getUnit());
            }
            BigDecimal price = l.unitPrice() != null ? l.unitPrice() : line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice();
            ret.getItems().add(SupplierReturnItem.builder().supplierReturn(ret).goodsReceiptItem(line).quantity(quantity)
                    .unitPrice(price).note(blankToNull(l.note())).build());
        }
        returnRepository.flush();
    }

    private void complete(SupplierReturn ret) {
        Staff staff = staffService.current();
        for (SupplierReturnItem item : ret.getItems()) {
            inventoryService.returnToSupplier(item.getGoodsReceiptItem().getBatch(), item.getQuantity(), staff,
                    "Trả hàng " + ret.getCode() + " · " + ret.getReason());
        }
        ret.setStatus(SupplierReturnStatus.COMPLETED);
        ret.setCompletedAt(LocalDateTime.now());
    }

    private SupplierReturn get(Long id) {
        return returnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu trả hàng: " + id));
    }

    private SupplierReturn draft(Long id) {
        SupplierReturn ret = get(id);
        if (ret.getStatus() != SupplierReturnStatus.DRAFT) throw new BadRequestException("Chỉ sửa / trả / hủy được phiếu tạm");
        return ret;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** TH + yyMMdd + running number of the day: TH261020-001 */
    private String newCode() {
        String prefix = "TH" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", returnRepository.countByCodeStartingWith(prefix) + 1);
    }
}
