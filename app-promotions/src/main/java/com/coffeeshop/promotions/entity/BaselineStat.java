package com.coffeeshop.promotions.entity;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaselineStat {

    private BigDecimal avgOrderValue;
    private BigDecimal ordersPerDay;
}
