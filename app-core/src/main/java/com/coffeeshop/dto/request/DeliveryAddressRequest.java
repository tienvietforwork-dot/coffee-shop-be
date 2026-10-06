package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(
        @Size(max = 30) String label,
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Size(max = 15) String recipientPhone,
        @NotBlank @Size(max = 255) String address) {
}
