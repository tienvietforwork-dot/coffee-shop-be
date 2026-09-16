package com.coffeeshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RevenueReportResponse {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private int totalOrders;
    private List<DailyPoint> daily;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DailyPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private int orders;
    }
}
