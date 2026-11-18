package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CustomerClassificationConfigRequest(
        @NotNull @DecimalMin("0") BigDecimal potentialSpendMin,
        @NotNull @DecimalMin("0") BigDecimal vipSpendMin,
        @NotNull @Positive Integer inactiveDays) { }
