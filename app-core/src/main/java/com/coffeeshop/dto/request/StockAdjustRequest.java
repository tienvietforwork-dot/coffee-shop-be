package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Kiểm kê: set a batch's remaining quantity to what was actually counted. */
public record StockAdjustRequest(
        @NotNull Long batchId,
        @NotNull @DecimalMin("0") BigDecimal actualQuantity,
        String note) {
}
