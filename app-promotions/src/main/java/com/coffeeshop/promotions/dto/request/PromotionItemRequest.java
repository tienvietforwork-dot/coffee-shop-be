package com.coffeeshop.promotions.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PromotionItemRequest {
    private Long id;
    private Long coffeeId;
    private BigDecimal customDiscount;
    private BigDecimal salePrice;
    private Integer maxQuantity;
    private Integer soldQuantity;
}