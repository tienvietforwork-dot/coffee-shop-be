package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Public menu: visible coffees grouped by category, with the price after running promotions. */
public record MenuResponse(List<MenuCategory> categories, List<OrderPromotion> orderPromotions) {

    public record MenuCategory(Long id, String name, String description, List<MenuCoffee> coffees) {
    }

    public record MenuCoffee(Long id, String name, String imageUrl, String description, BigDecimal price,
                             BigDecimal salePrice, String promotionName, boolean available) {
    }

    public record OrderPromotion(Long id, String name, String description, BigDecimal discountPercent,
                                 BigDecimal minOrderAmount, LocalDateTime endDate) {
    }
}
