package com.coffeeshop.stats.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductBundleAnalyticsResponse(Long bundleId, String bundleName, LocalDateTime from, LocalDateTime to,
                                             Integer estimatedSoldQuantity, BigDecimal estimatedRevenue,
                                             BigDecimal standalonePrice, BigDecimal bundlePrice,
                                             BigDecimal estimatedSavingGiven,
                                             List<ComponentSales> components) {

    public record ComponentSales(Long coffeeId, String coffeeName, Integer requiredQuantity,
                                 boolean optional, Integer soldQuantity, BigDecimal revenue) {
    }
}
