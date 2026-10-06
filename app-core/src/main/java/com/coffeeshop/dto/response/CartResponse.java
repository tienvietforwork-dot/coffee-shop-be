package com.coffeeshop.dto.response;

import com.coffeeshop.entity.enums.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(String sessionCode, CartStatus status, Long tableId, String tableNo,
                           List<Item> items, BigDecimal subtotal) {

    public record Item(Long id, Long coffeeId, String coffeeName, String imageUrl, Integer quantity,
                       BigDecimal unitPrice, String note, BigDecimal subtotal, boolean available) {
    }
}
