package com.coffeeshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Phiếu đặt hàng nháp; lines replaced as a whole on every save. {@code paymentTermDays} null: the supplier's terms;
 * {@code vatRate} in % (0, 5, 8, 10), null when the supplier issues no VAT invoice.
 */
public record PurchaseOrderRequest(
        @NotNull Long supplierId,
        LocalDate expectedDate,
        @Size(max = 255) String note,
        @Min(0) @Max(365) Integer paymentTermDays,
        @DecimalMin("0") BigDecimal discountAmount,
        @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,
        @DecimalMin("0") BigDecimal shippingCost,
        @NotEmpty List<@Valid Line> items) {

    /**
     * {@code packageId} + {@code packageCount} (10 × "Hộp 500 ml") or a loose {@code quantity} in the material's unit;
     * {@code unitPrice} per unit of the material.
     */
    public record Line(
            @NotNull Long materialId,
            Long packageId,
            @DecimalMin("0.01") BigDecimal packageCount,
            @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0") BigDecimal unitPrice,
            @Size(max = 255) String note) {
    }
}
