package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Start {@code batches} batches (may be fractional, e.g. 0.5) of a prepared material. */
public record ProduceRequest(@NotNull Long materialId, @NotNull @DecimalMin("0.1") BigDecimal batches,
                             @Size(max = 255) String note) {
}
