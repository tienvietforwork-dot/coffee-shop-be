package com.coffeeshop.dto.response;

import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.GoodsReceiptItem;
import com.coffeeshop.entity.Money;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phiếu nhập kho (mẫu 01-VT). Amounts: {@code totalAmount} = tiền hàng − giảm giá + thuế GTGT + chi phí nhập;
 * {@code paidAmount} / {@code returnedAmount} / {@code remainingAmount} come from the supplier's công nợ
 * (payments are applied to the oldest receipts first, a payment made on the receipt goes to it first).
 */
public record GoodsReceiptResponse(Long id, String code, GoodsReceiptStatus status, Long supplierId, String supplierName,
                                   String supplierTaxCode, String supplierAddress, Long purchaseOrderId,
                                   String purchaseOrderCode, String staffName, String delivererName,
                                   String invoiceSeries, String invoiceNo, LocalDate invoiceDate, Long documentImageId,
                                   BigDecimal goodsAmount, BigDecimal discountAmount, BigDecimal vatRate,
                                   BigDecimal vatAmount, BigDecimal shippingCost, BigDecimal totalAmount,
                                   BigDecimal paidAmount, BigDecimal returnedAmount, BigDecimal remainingAmount,
                                   LocalDate dueDate, boolean overdue, String note, String cancelReason,
                                   LocalDateTime cancelledAt, LocalDateTime createdAt, String createdBy, List<Line> lines) {

    /** {@code remainingInLot}: still in stock from this line's lot (what can still be returned) */
    public record Line(Long id, Long materialId, String materialName, String unit, Long purchaseOrderItemId,
                       Long batchId, String packageName, BigDecimal packageSize, BigDecimal documentQuantity,
                       BigDecimal receivedQuantity, BigDecimal rejectedQuantity, BigDecimal shortQuantity,
                       String rejectReason, BigDecimal unitPrice, BigDecimal amount, LocalDate expiryDate,
                       BigDecimal temperature, BigDecimal remainingInLot, String note) {
        static Line from(GoodsReceiptItem i) {
            var b = i.getBatch();
            return new Line(i.getId(), i.getMaterial().getId(), i.getMaterial().getName(), i.getMaterial().getUnit(),
                    i.getPurchaseOrderItem() == null ? null : i.getPurchaseOrderItem().getId(),
                    b == null ? null : b.getId(), i.getPackageName(), i.getPackageSize(), i.getDocumentQuantity(),
                    i.getReceivedQuantity(), i.getRejectedQuantity(), i.shortQuantity(), i.getRejectReason(),
                    i.getUnitPrice(), i.amount(), i.getExpiryDate(), i.getTemperature(),
                    b == null ? BigDecimal.ZERO : b.getRemainingQuantity(), i.getNote());
        }
    }

    /** What has been settled on the receipt (see SupplierDebtService). */
    public record Settlement(BigDecimal paid, BigDecimal returned) {
        public static final Settlement NONE = new Settlement(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static GoodsReceiptResponse from(GoodsReceipt r, Settlement s) {
        var po = r.getPurchaseOrder();
        var supplier = r.getSupplier();
        BigDecimal total = r.payable();
        BigDecimal remaining = total.subtract(s.paid()).subtract(s.returned()).max(BigDecimal.ZERO);
        return new GoodsReceiptResponse(r.getId(), r.getCode(), r.getStatus(), supplier.getId(), supplier.getName(),
                supplier.getTaxCode(), supplier.getAddress(), po == null ? null : po.getId(),
                po == null ? null : po.getCode(), r.getStaff() == null ? null : r.getStaff().getFullName(),
                r.getDelivererName(), r.getInvoiceSeries(), r.getInvoiceNo(), r.getInvoiceDate(), r.getDocumentImageId(),
                r.goodsAmount(), r.getDiscountAmount(), r.getVatRate(),
                Money.vat(r.goodsAmount(), r.getDiscountAmount(), r.getVatRate()), r.getShippingCost(), total,
                s.paid(), s.returned(), remaining, r.getDueDate(),
                remaining.signum() > 0 && r.getDueDate().isBefore(LocalDate.now()), r.getNote(), r.getCancelReason(),
                r.getCancelledAt(), r.getCreatedAt(), r.getCreatedBy(), r.getItems().stream().map(Line::from).toList());
    }
}
