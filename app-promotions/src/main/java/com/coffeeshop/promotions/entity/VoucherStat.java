package com.coffeeshop.promotions.entity;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherStat {

    private Long issued;
    private Long used;
    private Long expired;
    private Double usageRate;
    private BigDecimal discountTotal;
}
