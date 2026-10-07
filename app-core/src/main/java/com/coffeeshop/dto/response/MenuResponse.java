package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Public menu: visible coffees grouped by category, with the price after running promotions.
 * {@code stock} (material id → quantity) and each coffee's {@code perCup} let the client work out,
 * while the cart fills up, how many more cups it can still add — without a request per click.
 */
public record MenuResponse(List<MenuCategory> categories, List<OrderPromotion> orderPromotions, Map<Long, BigDecimal> stock) {

    public record MenuCategory(Long id, String name, String description, List<MenuCoffee> coffees) {
    }

    /** {@code servings}: cups the stock allows (null = not limited); {@code available} is false at 0. */
    public record MenuCoffee(Long id, String name, String imageUrl, String description, BigDecimal price,
                             BigDecimal salePrice, String promotionName, boolean available,
                             Integer servings, Map<Long, BigDecimal> perCup) {
    }

    public record OrderPromotion(Long id, String name, String description, BigDecimal discountPercent,
                                 BigDecimal minOrderAmount, LocalDateTime endDate) {
    }
}
