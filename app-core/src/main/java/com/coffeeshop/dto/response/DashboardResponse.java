package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(long todayOrders, BigDecimal todayRevenue, long activeOrders, long lowStockCount,
                                long expiringBatchCount, List<TopCoffee> topCoffeesThisWeek) {

    public record TopCoffee(Long coffeeId, String name, long quantity) {
    }
}
