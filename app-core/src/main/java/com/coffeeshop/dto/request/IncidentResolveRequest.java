package com.coffeeshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Resolve an incident (the logged-in employee is recorded as handled_by); optionally compensate the
 * customer with {@code voucherCount} vouchers worth {@code voucherValue} each.
 */
public record IncidentResolveRequest(
        @NotBlank String resolution,
        @DecimalMin("1000") BigDecimal voucherValue,
        @DecimalMin("0") BigDecimal voucherMinOrderValue,
        @Min(1) @Max(5) Integer voucherCount,
        @Min(1) Integer voucherValidDays) {
}
