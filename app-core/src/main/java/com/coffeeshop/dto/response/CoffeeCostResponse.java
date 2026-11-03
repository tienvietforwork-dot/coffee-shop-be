package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Giá vốn của 1 ly theo công thức đang dùng: Σ định lượng × giá vốn nguyên liệu (bình quân các lô còn trong kho,
 * hết hàng thì giá lần nhập gần nhất; nguyên liệu chế biến = định mức thô ÷ định lượng ra). {@code complete} false:
 * some material has no known cost yet. {@code previousCost}: last recorded cost before this one (for the trend).
 */
public record CoffeeCostResponse(Long coffeeId, String coffeeName, String categoryName, String status, Long recipeId,
                                 BigDecimal price, BigDecimal unitCost, BigDecimal grossMargin, BigDecimal foodCostPercent,
                                 boolean complete, BigDecimal previousCost, LocalDateTime previousAt, List<Line> lines) {

    public record Line(Long materialId, String materialName, String unit, BigDecimal quantity, BigDecimal unitCost,
                       BigDecimal cost, String costSource) {
    }

    public record History(LocalDateTime at, BigDecimal unitCost, BigDecimal price) {
    }
}
