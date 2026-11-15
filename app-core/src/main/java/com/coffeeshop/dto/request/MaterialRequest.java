package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** quantity: số lượng hiện có (để trống khi sửa = giữ nguyên) */
public record MaterialRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String unit,
        @PositiveOrZero BigDecimal quantity) {
}
