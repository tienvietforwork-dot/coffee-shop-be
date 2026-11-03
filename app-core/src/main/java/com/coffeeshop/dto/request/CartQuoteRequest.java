package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.constraints.NotNull;

/** {@code deliveryDistrict}: quận / phường for a delivery, priced by its khu vực giao hàng */
public record CartQuoteRequest(@NotNull OrderType orderType, String customerPhone, String voucherCode, String deliveryDistrict) {
}
