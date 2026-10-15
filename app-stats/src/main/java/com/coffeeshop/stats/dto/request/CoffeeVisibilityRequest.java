package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import jakarta.validation.constraints.NotNull;

public record CoffeeVisibilityRequest(@NotNull CoffeeStatus status) {
}
