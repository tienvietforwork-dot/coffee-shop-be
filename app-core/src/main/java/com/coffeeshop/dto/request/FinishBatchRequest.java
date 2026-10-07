package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Finish a PREPARING lot with what was actually obtained; {@code expiresAt} overrides the standard shelf life for this lot.
 * Discarding uses only {@code note}.
 */
public record FinishBatchRequest(@DecimalMin("0.01") BigDecimal actualQuantity, LocalDateTime expiresAt,
                                 @Size(max = 255) String note) {
}
