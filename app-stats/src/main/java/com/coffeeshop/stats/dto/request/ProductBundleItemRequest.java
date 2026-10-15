package com.coffeeshop.stats.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductBundleItemRequest(
        @NotNull Long coffeeId,
        @NotNull @Min(1) Integer quantity,
        Boolean optional,
        @Min(0) Integer displayOrder) {
}
