package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.enums.CoffeeStatus;

import java.math.BigDecimal;

public record CoffeeResponse(Long id, Long categoryId, String categoryName, String code, String name, Long imageId,
                             String imageUrl, BigDecimal price, String description, CoffeeStatus status, boolean hasRecipe,
                             boolean autoSoldOut) {
    public static CoffeeResponse from(Coffee c, boolean hasRecipe) {
        return new CoffeeResponse(c.getId(), c.getCategory().getId(), c.getCategory().getName(), c.getCode(), c.getName(),
                c.getImageId(), c.imageSrc(), c.getPrice(), c.getDescription(), c.getStatus(), hasRecipe, c.isAutoSoldOut());
    }
}
