package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.TableStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiningTableRequest(
        @NotBlank @Size(max = 10) String tableNo,
        @Size(max = 50) String area,
        @Min(1) Integer capacity,
        TableStatus status) {
}
