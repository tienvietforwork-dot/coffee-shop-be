package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Phiếu nhập kho: one delivery, against a purchase order ({@code POST /api/purchase-orders/{id}/receipts}, lines name
 * the order's {@code itemId}) or loose ({@code POST /api/goods-receipts}, {@code supplierId} required and lines name
 * {@code materialId} + optional {@code packageId}).
 * <p>
 * Counts are in the line's packaging (Hộp, Bao…) or, for loose units, in the material's unit:
 * {@code documentCount} as written on the supplier's delivery note, {@code receivedCount} counted and accepted,
 * {@code rejectedCount} refused at the door with {@code rejectReason}.
 * {@code paidAmount}: paid to the supplier on the spot (a phiếu chi is recorded); the rest goes to công nợ.
 */
public record GoodsReceiptRequest(
        Long supplierId,
        @Size(max = 100) String delivererName,
        @Size(max = 20) String invoiceSeries,
        @Size(max = 20) String invoiceNo,
        LocalDate invoiceDate,
        Long documentImageId,
        @DecimalMin("0") BigDecimal discountAmount,
        @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,
        @DecimalMin("0") BigDecimal shippingCost,
        @DecimalMin("0") BigDecimal paidAmount,
        PaymentMethod paymentMethod,
        @Size(max = 100) String paymentReference,
        @Size(max = 255) String note,
        @NotEmpty List<@Valid Line> lines) {

    public record Line(
            Long itemId,
            Long materialId,
            Long packageId,
            @DecimalMin("0") BigDecimal documentCount,
            @NotNull @DecimalMin("0") BigDecimal receivedCount,
            @DecimalMin("0") BigDecimal rejectedCount,
            @Size(max = 255) String rejectReason,
            @DecimalMin("0") BigDecimal unitPrice,
            LocalDate expiryDate,
            @DecimalMin("-50") @DecimalMax("60") BigDecimal temperature,
            @Size(max = 255) String note) {
    }
}
