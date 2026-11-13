package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.coffeeshop.entity.enums.CategoryProductType;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull CategoryProductType productType,
        Integer displayOrder) {
}
