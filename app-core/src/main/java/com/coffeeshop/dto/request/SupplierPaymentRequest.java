package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phiếu chi trả nhà cung cấp. {@code goodsReceiptId}: trả cho phiếu nhập đó (applied to it first);
 * {@code purchaseOrderId}: trả trước / đặt cọc cho phiếu đặt; neither: trả nợ, oldest receipts first.
 */
public record SupplierPaymentRequest(
        @NotNull Long supplierId,
        @NotNull @DecimalMin("1") BigDecimal amount,
        @NotNull PaymentMethod method,
        @Size(max = 100) String reference,
        @Size(max = 100) String payeeName,
        LocalDateTime paidAt,
        Long goodsReceiptId,
        Long purchaseOrderId,
        Long documentImageId,
        @Size(max = 255) String note) {
}
