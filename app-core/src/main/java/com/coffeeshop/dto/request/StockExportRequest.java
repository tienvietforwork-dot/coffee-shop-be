package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockExportRequest(
        @NotNull Long materialId,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        String note) {
}
