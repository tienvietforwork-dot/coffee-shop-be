package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.Coffee;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;

import java.math.BigDecimal;

public record CoffeePlacementResponse(Long id, Long categoryId, String categoryName, String name, Long imageId,
                                      String imageUrl, BigDecimal price, String description, CoffeeStatus status,
                                      boolean autoSoldOut) {

    public static CoffeePlacementResponse from(Coffee coffee) {
        return new CoffeePlacementResponse(coffee.getId(), coffee.getCategory().getId(), coffee.getCategory().getName(),
                coffee.getName(), coffee.getImageId(), coffee.imageSrc(), coffee.getPrice(), coffee.getDescription(),
                coffee.getStatus(), coffee.isAutoSoldOut());
    }
}
