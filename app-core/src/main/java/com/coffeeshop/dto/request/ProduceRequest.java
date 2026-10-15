package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Record {@code batches} batches (may be fractional, e.g. 0.5) of a prepared material as made;
 * {@code expiresAt} overrides the standard shelf life for this lot;
 * {@code lots}: component id -> the lot to take it from first (default FEFO).
 */
public record ProduceRequest(@NotNull Long materialId, @NotNull @DecimalMin("0.1") BigDecimal batches,
                             LocalDateTime expiresAt, @Size(max = 255) String note, Map<Long, Long> lots) {
}
