package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PromotionStat {

    private Long id;
    private String name;
    private PromotionStatus status;
    private Long orders;
    private BigDecimal revenue;
    private BigDecimal discountTotal;
    private BigDecimal avgOrderValue;
    private BigDecimal roi;
}
