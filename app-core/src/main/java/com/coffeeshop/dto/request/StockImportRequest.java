package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockImportRequest(
        @NotNull Long materialId,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @DecimalMin("0") BigDecimal unitCost,
        LocalDate expiryDate,
        String note) {
}
