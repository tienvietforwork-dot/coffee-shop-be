package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.constraints.NotNull;

public record CartQuoteRequest(@NotNull OrderType orderType, String customerPhone, String voucherCode) {
}
