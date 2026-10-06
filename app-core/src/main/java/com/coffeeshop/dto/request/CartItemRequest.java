package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartItemRequest(
        @NotNull Long coffeeId,
        @NotNull @Min(1) @Max(99) Integer quantity,
        @Size(max = 255) String note) {
}
