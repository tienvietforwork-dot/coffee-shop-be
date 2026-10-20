package com.coffeeshop.dto.response;

import com.coffeeshop.entity.PurchaseOrder;
import com.coffeeshop.entity.PurchaseOrderItem;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** {@code receipts}: only on the detail (GET by id). Amounts = quantity × unit price per unit of the material. */
public record PurchaseOrderResponse(Long id, String code, Long supplierId, String supplierName, String supplierPhone,
                                    String supplierAddress, String staffName, PurchaseOrderStatus status,
                                    LocalDate expectedDate, LocalDateTime orderedAt, LocalDateTime completedAt,
                                    String note, String closeReason, LocalDateTime createdAt, String createdBy,
                                    BigDecimal totalAmount, List<Item> items, List<GoodsReceiptResponse> receipts) {

    public record Item(Long id, Long materialId, String materialName, String unit, String packageName,
                       BigDecimal packageSize, BigDecimal packageCount, BigDecimal quantity, BigDecimal receivedQuantity,
                       BigDecimal unitPrice, BigDecimal amount, String note) {
        static Item from(PurchaseOrderItem i) {
            return new Item(i.getId(), i.getMaterial().getId(), i.getMaterial().getName(), i.getMaterial().getUnit(),
                    i.getPackageName(), i.getPackageSize(), i.getPackageCount(), i.getQuantity(), i.getReceivedQuantity(),
                    i.getUnitPrice(), lineAmount(i), i.getNote());
        }
    }

    private static BigDecimal lineAmount(PurchaseOrderItem i) {
        return i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice().multiply(i.getQuantity());
    }

    public static PurchaseOrderResponse from(PurchaseOrder p) {
        return from(p, null);
    }

    public static PurchaseOrderResponse from(PurchaseOrder p, List<GoodsReceiptResponse> receipts) {
        var s = p.getSupplier();
        return new PurchaseOrderResponse(p.getId(), p.getCode(), s.getId(), s.getName(), s.getPhone(), s.getAddress(),
                p.getStaff() == null ? null : p.getStaff().getFullName(), p.getStatus(), p.getExpectedDate(),
                p.getOrderedAt(), p.getCompletedAt(), p.getNote(), p.getCloseReason(), p.getCreatedAt(), p.getCreatedBy(),
                p.getItems().stream().map(PurchaseOrderResponse::lineAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                p.getItems().stream().map(Item::from).toList(), receipts);
    }
}
