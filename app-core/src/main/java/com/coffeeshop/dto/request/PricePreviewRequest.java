package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PricePreviewRequest(
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotNull OrderType orderType,
        String customerPhone,
        String voucherCode) {
}
