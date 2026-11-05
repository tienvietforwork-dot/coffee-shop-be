package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.constraints.NotNull;

/** {@code deliveryAddress}: địa chỉ giao tới — đo km từ quán → phí ship; {@code deliveryTime}: giờ giao hẹn (null = ngay) → khung cao điểm */
public record CartQuoteRequest(@NotNull OrderType orderType, String customerPhone, String voucherCode, String deliveryAddress,
                               java.time.LocalDateTime deliveryTime) {
}
