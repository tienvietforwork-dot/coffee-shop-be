package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.CoffeeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CoffeeRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 150) String name,
        /** uploaded photo (POST /api/images); when set, imageUrl is ignored */
        Long imageId,
        @Size(max = 500) String imageUrl,
        @NotNull @DecimalMin("0") BigDecimal price,
        String description,
        CoffeeStatus status) {
}
