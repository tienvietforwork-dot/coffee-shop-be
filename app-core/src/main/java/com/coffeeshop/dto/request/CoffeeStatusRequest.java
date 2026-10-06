package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.CoffeeStatus;
import jakarta.validation.constraints.NotNull;

public record CoffeeStatusRequest(@NotNull CoffeeStatus status) {
}
