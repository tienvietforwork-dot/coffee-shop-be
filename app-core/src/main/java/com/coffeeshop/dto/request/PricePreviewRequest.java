package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PricePreviewRequest(
        @Valid List<CartItemRequest> items,
        @Valid List<BundleLineRequest> bundles,
        @NotNull OrderType orderType,
        String customerPhone,
        String voucherCode,
        String deliveryAddress,
        java.time.LocalDateTime deliveryTime) {
}
