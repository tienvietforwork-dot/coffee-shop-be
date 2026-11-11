package com.coffeeshop.promotions.dto.response;


import com.coffeeshop.promotions.entity.BaselineStat;
import com.coffeeshop.promotions.entity.PromotionStat;
import com.coffeeshop.promotions.entity.VoucherStat;
import lombok.*;

import java.util.List;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PromotionAnalyticsResponse {

    private List<PromotionStat> promotions;
    private VoucherStat vouchers;
    private BaselineStat baseline;


}
