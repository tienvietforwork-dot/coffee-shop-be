package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaRuleDTO {
    @DecimalMin(value = "0.0", inclusive = true)
    @JsonAlias("min_spend")
    private BigDecimal minSpend;

    @DecimalMin(value = "0.0", inclusive = true)
    @JsonAlias("max_spend")
    private BigDecimal maxSpend;

    @Positive
    @JsonAlias({"min_orders", "min_freq"})
    private Integer minOrders;

    @Positive
    @JsonAlias({"lastOrderDays", "last_order_days", "max_recency_days"})
    private Integer inactiveDays;

    @Positive
    @JsonAlias("favorite_category_id")
    private Long favoriteCategoryId;

    @Positive
    @JsonAlias("favorite_coffee_id")
    private Long favoriteCoffeeId;
}
