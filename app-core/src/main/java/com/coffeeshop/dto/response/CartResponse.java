package com.coffeeshop.dto.response;

import com.coffeeshop.entity.enums.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(String sessionCode, CartStatus status, Long tableId, String tableNo,
                           List<Item> items, BigDecimal subtotal, List<com.coffeeshop.service.BundleCartService.CartBundle> bundles) {

    public record Item(Long id, Long coffeeId, String coffeeName, String imageUrl, Integer quantity,
                       BigDecimal unitPrice, String note, BigDecimal subtotal, boolean available,
                       java.util.List<OptionLine> options) {
    }
    public record OptionLine(Long id, String name, String kind, BigDecimal extraPrice) {}
}
