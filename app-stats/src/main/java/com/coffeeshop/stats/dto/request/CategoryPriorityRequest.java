package com.coffeeshop.stats.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryPriorityRequest(
        @NotNull(message = "Nhập thứ tự ưu tiên")
        @Min(value = 0, message = "Thứ tự ưu tiên phải là số nguyên không âm")
        Integer displayOrder) {
}
