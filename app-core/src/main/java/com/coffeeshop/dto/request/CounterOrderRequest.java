package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Order taken by staff at the counter: single coffees ({@code items}) and / or combos ({@code bundles}). */
public record CounterOrderRequest(
        @Valid List<CartItemRequest> items,
        @Valid List<BundleLineRequest> bundles,
        @NotNull OrderType orderType,
        Long tableId,
        @Size(max = 100) String customerName,
        @Pattern(regexp = "^0\\d{9,10}$", message = "invalid phone number") String customerPhone,
        @Valid DeliveryAddressRequest delivery,
        @NotNull PaymentMethod paymentMethod,
        /** true = money collected right away at the counter. */
        Boolean paidNow,
        @Size(max = 30) String voucherCode,
        String note) {
}
