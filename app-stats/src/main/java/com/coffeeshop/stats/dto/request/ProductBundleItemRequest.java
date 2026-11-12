package com.coffeeshop.stats.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductBundleItemRequest(
        @NotNull @Positive Long coffeeId,
        @NotNull @Min(1) @Max(99) Integer quantity,
        Boolean optional,
        @Min(0) Integer displayOrder,
        @Size(max = 80) String choiceGroup) {
}
