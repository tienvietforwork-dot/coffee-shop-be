package com.coffeeshop.promotions.dto.request;

import com.coffeeshop.promotions.entity.enums.PromotionScope;
import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import com.coffeeshop.promotions.entity.enums.PromotionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {
    private String name;
    private String description;
    private PromotionScope applyScope;
    private PromotionType discountType;
    private BigDecimal discountPercent;
    private BigDecimal minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PromotionStatus status;
    private List<PromotionItemRequest> items;
}