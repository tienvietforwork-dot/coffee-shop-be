package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Customer checkout of a QR / online cart. */
public record CheckoutRequest(
        @NotBlank String sessionCode,
        @NotNull OrderType orderType,
        @Size(max = 100) String customerName,
        @Pattern(regexp = "^0\\d{9,10}$", message = "invalid phone number") String customerPhone,
        Long addressId,
        @Valid DeliveryAddressRequest delivery,
        LocalDateTime pickupTime,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 30) String voucherCode,
        String note) {
}
