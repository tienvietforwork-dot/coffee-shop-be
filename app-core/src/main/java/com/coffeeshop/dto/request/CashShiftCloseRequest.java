package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Chốt ca. {@code denominations}: mệnh giá → số tờ (500000 → 2 …); the counted cash is their sum.
 * {@code nextOpeningCash}: để lại két cho ca sau; the rest is handed over. A difference needs a reason.
 */
public record CashShiftCloseRequest(
        @NotNull Map<Integer, @Min(0) Integer> denominations,
        @DecimalMin("0") BigDecimal nextOpeningCash,
        @Size(max = 100) String receivedBy,
        @Size(max = 255) String differenceReason,
        @Size(max = 255) String note) {
}
