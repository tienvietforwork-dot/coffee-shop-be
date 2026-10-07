package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.service.StockService;

import java.math.BigDecimal;
import java.util.List;

/** {@code servings}: cups the stock allows (null = not limited); {@code stock}: the per-material working behind it. */
public record CoffeeResponse(Long id, Long categoryId, String categoryName, String name, Long imageId,
                             String imageUrl, BigDecimal price, String description, CoffeeStatus status, boolean autoSoldOut, boolean hasRecipe,
                             Integer servings, List<StockService.StockLine> stock) {
    public static CoffeeResponse from(Coffee c, StockService.Snapshot snap) {
        return new CoffeeResponse(c.getId(), c.getCategory().getId(), c.getCategory().getName(), c.getName(),
                c.getImageId(), c.imageSrc(), c.getPrice(), c.getDescription(), c.getStatus(), c.isAutoSoldOut(), snap.hasRecipe(c.getId()),
                snap.servings(c.getId()), snap.lines(c.getId()));
    }
}
