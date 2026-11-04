package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String unit) {
}
