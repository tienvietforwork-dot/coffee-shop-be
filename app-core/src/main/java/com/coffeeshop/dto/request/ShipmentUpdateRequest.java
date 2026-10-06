package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShipmentUpdateRequest(
        @NotNull ShipmentStatus status,
        @Size(max = 50) String carrier,
        @Size(max = 100) String trackingCode,
        String note) {
}
