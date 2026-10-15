package com.coffeeshop.stats.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryPriorityRequest(@NotNull @Min(0) Integer displayOrder) {
}
