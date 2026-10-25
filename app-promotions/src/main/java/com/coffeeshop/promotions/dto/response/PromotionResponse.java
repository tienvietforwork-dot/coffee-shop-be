package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.Promotion;
import com.coffeeshop.promotions.entity.enums.PromotionScope;
import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import com.coffeeshop.promotions.entity.enums.PromotionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class PromotionResponse {
    private Long id;
    private String name;
    private String description;
    private PromotionScope applyScope;
    private PromotionType discountType;
    private BigDecimal discountPercent;
    private BigDecimal minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PromotionStatus status;
    private List<PromotionItemResponse> items;

    public  PromotionResponse(Promotion promotion) {
        this.id = promotion.getId();
        this.name = promotion.getName();
        this.description = promotion.getDescription();
        this.applyScope = promotion.getApplyScope();
        this.discountType = promotion.getDiscountType();
        this.discountPercent = promotion.getDiscountPercent();
        this.minOrderAmount = promotion.getMinOrderAmount();
        this.startDate = promotion.getStartDate();
        this.endDate = promotion.getEndDate();
        this.status = promotion.getStatus();

        if (promotion.getPromotionItems() != null) {
            this.items = promotion.getPromotionItems().stream()
                    .map(PromotionItemResponse::new)
                    .collect(Collectors.toList());
        }
    }
}