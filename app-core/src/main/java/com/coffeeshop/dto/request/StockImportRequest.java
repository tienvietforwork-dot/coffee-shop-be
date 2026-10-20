package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nhập lẻ (không theo phiếu đặt): either {@code packageId} + {@code packageCount} (10 × "Hộp 500 ml" = 5000 ml)
 * or a loose {@code quantity} in the material's unit. The supplier is required.
 */
public record StockImportRequest(
        @NotNull Long supplierId,
        @NotNull Long materialId,
        Long packageId,
        @DecimalMin("0.01") BigDecimal packageCount,
        @DecimalMin("0.01") BigDecimal quantity,
        @DecimalMin("0") BigDecimal unitCost,
        LocalDate expiryDate,
        String note) {
}
