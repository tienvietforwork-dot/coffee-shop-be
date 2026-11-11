package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.constraints.NotNull;

/** {@code deliveryAddress}: địa chỉ giao tới — tỉnh / phường quyết định phí ship; {@code deliveryTime}: giờ giao hẹn (null = ngay) */
public record CartQuoteRequest(@NotNull OrderType orderType, String customerPhone, String voucherCode, String deliveryAddress,
                               java.time.LocalDateTime deliveryTime) {
}
