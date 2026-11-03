package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.PromotionItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PromotionItemResponse {
    private Long id;
    private Long coffeeId;
    private String coffeeName;
    private BigDecimal customDiscount;
    private BigDecimal salePrice;
    private Integer maxQuantity;
    private Integer soldQuantity;

    public PromotionItemResponse(PromotionItem item) {
        this.id = item.getId();
        if (item.getCoffee() != null) {
            this.coffeeId = item.getCoffee().getId();
            this.coffeeName = item.getCoffee().getName();
        }
        this.customDiscount = item.getCustomDiscount();
        this.salePrice = item.getSalePrice();
        this.maxQuantity = item.getMaxQuantity();
        this.soldQuantity = item.getSoldQuantity();
    }
}