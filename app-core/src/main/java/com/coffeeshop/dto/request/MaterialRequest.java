package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.MaterialStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MaterialRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String unit,
        @DecimalMin("0") BigDecimal minStock,
        MaterialStatus status) {
}
