package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Resolve an incident (the logged-in employee is recorded as handled_by); optionally compensate the
 * customer with {@code voucherCount} vouchers of type {@code voucherId}, each holding one slot of its quantity.
 * Each voucher expires after the type's own valid_days, counted from the moment it is issued (no override).
 */
public record IncidentResolveRequest(
        @NotBlank String resolution,
        Long voucherId,
        @Min(1) @Max(5) Integer voucherCount) {
}
