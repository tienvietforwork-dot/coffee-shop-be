package com.coffeeshop.service;

import com.coffeeshop.dto.request.GoodsReceiptRequest;
import com.coffeeshop.dto.request.PurchaseOrderRequest;
import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.dto.response.PurchaseOrderResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialPackage;
import com.coffeeshop.entity.PurchaseOrder;
import com.coffeeshop.entity.PurchaseOrderItem;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.PurchaseOrderRepository;
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
import java.util.Set;

/**
 * Phiếu đặt hàng nhà cung cấp: DRAFT → ORDERED → PARTIALLY_RECEIVED → COMPLETED, or CANCELLED.
 * Goods arrive as phiếu nhập (one per delivery); each received line becomes a lot.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;
    private final GoodsReceiptService receiptService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> search(PurchaseOrderStatus status, Long supplierId) {
        return orderRepository.search(status, supplierId).stream().map(PurchaseOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse find(Long id) {
        PurchaseOrder order = get(id);
        return PurchaseOrderResponse.from(order, receiptService.search(null, id, null, null, null));
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        PurchaseOrder order = orderRepository.save(PurchaseOrder.builder()
                .code(newCode()).supplier(supplierService.active(request.supplierId())).staff(staffService.current()).build());
        apply(order, request);
        return PurchaseOrderResponse.from(order);
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request) {
        PurchaseOrder order = draft(id, "sửa");
        order.setSupplier(supplierService.active(request.supplierId()));
        apply(order, request);
        return PurchaseOrderResponse.from(order);
    }

    /** Only drafts: once sent, the order is cancelled or closed instead, so the history stays. */
    @Transactional
    public void delete(Long id) {
        PurchaseOrder order = draft(id, "xóa");
        order.getItems().forEach(PurchaseOrderItem::softDelete);
        order.softDelete();
    }

    /** Gửi nhà cung cấp: DRAFT → ORDERED. */
    @Transactional
    public PurchaseOrderResponse send(Long id) {
        PurchaseOrder order = draft(id, "gửi");
        supplierService.active(order.getSupplier().getId());
        order.setStatus(PurchaseOrderStatus.ORDERED);
        order.setOrderedAt(LocalDateTime.now());
        return PurchaseOrderResponse.from(order);
    }

    /** Hủy: an order already sent but nothing received yet. */
    @Transactional
    public PurchaseOrderResponse cancel(Long id, String reason) {
        PurchaseOrder order = get(id);
        if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new BadRequestException(order.getStatus() == PurchaseOrderStatus.DRAFT
                    ? "Phiếu nháp thì xóa, không cần hủy" : "Chỉ hủy được phiếu đã đặt và chưa nhận hàng");
        }
        finish(order, PurchaseOrderStatus.CANCELLED, reason);
        return PurchaseOrderResponse.from(order);
    }

    /** Đóng phiếu: the supplier will not deliver the rest; accept what was received. */
    @Transactional
    public PurchaseOrderResponse close(Long id, String reason) {
        PurchaseOrder order = get(id);
        if (order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Chỉ đóng được phiếu đã nhận một phần");
        }
        finish(order, PurchaseOrderStatus.COMPLETED, reason);
        return PurchaseOrderResponse.from(order);
    }

    /** Phiếu nhập for one delivery: each accepted line becomes a lot; the order follows what has come in. */
    @Transactional
    public GoodsReceiptResponse receive(Long id, GoodsReceiptRequest request) {
        return receiptService.receiveForOrder(get(id), request);
    }

    private void finish(PurchaseOrder order, PurchaseOrderStatus status, String reason) {
        order.setStatus(status);
        order.setCompletedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) order.setCloseReason(reason.trim());
    }

    private PurchaseOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu đặt hàng: " + id));
    }

    private PurchaseOrder draft(Long id, String action) {
        PurchaseOrder order = get(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BadRequestException("Chỉ " + action + " được phiếu nháp");
        }
        return order;
    }

    private void apply(PurchaseOrder order, PurchaseOrderRequest request) {
        if (request.expectedDate() != null && request.expectedDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Ngày hẹn giao đã qua");
        }
        order.setExpectedDate(request.expectedDate());
        order.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        order.setPaymentTermDays(request.paymentTermDays() != null ? request.paymentTermDays() : order.getSupplier().getPaymentTermDays());
        order.setDiscountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        order.setVatRate(request.vatRate());
        order.setShippingCost(request.shippingCost() == null ? BigDecimal.ZERO : request.shippingCost());
        order.getItems().forEach(PurchaseOrderItem::softDelete);
        order.getItems().clear();
        Set<Long> seen = new HashSet<>();
        for (PurchaseOrderRequest.Line line : request.items()) {
            Material material = inventoryService.purchasable(line.materialId());
            if (!seen.add(material.getId())) throw new BadRequestException("Nguyên liệu bị lặp: " + material.getName());
            MaterialPackage pkg = inventoryService.packageOf(material, line.packageId());
            BigDecimal quantity = line.quantity();
            if (pkg != null) {
                if (line.packageCount() == null) throw new BadRequestException("Nhập số " + pkg.getName() + " của '" + material.getName() + "'");
                quantity = pkg.getSize().multiply(line.packageCount()).setScale(2, RoundingMode.HALF_UP);
            }
            if (quantity == null) throw new BadRequestException("Nhập số lượng đặt của '" + material.getName() + "'");
            order.getItems().add(PurchaseOrderItem.builder().purchaseOrder(order).material(material)
                    .packageName(pkg == null ? null : pkg.getName())
                    .packageSize(pkg == null ? null : pkg.getSize())
                    .packageCount(pkg == null ? null : line.packageCount())
                    .quantity(quantity).unitPrice(line.unitPrice())
                    .note(line.note() == null || line.note().isBlank() ? null : line.note().trim()).build());
        }
        if (order.getDiscountAmount().compareTo(order.goodsAmount()) > 0) throw new BadRequestException("Giảm giá lớn hơn tiền hàng");
        orderRepository.flush(); // the new lines get their ids, which the response carries
    }

    /** DH + yyMMdd + running number of the day: DH261020-001 */
    private String newCode() {
        String prefix = "DH" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", orderRepository.countByCodeStartingWith(prefix) + 1);
    }
}
