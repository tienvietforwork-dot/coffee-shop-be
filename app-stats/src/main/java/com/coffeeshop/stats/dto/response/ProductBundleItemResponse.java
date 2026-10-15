package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.ProductBundleItem;

import java.math.BigDecimal;

public record ProductBundleItemResponse(Long id, Integer quantity, boolean optional, Integer displayOrder,
                                        BigDecimal linePrice, CoffeePlacementResponse coffee) {

    public static ProductBundleItemResponse from(ProductBundleItem item) {
        BigDecimal linePrice = item.getCoffee().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new ProductBundleItemResponse(item.getId(), item.getQuantity(), item.isOptional(),
                item.getDisplayOrder(), linePrice, CoffeePlacementResponse.from(item.getCoffee()));
    }
}
