package com.coffeeshop.scheduler;

import com.coffeeshop.service.RevenueAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Nightly job that pre-computes yesterday's revenue into the daily_revenue
 * table so that /api/reports/revenue can serve historical ranges quickly
 * without re-scanning the orders table each time.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RevenueAggregationScheduler {

    private final RevenueAggregationService revenueAggregationService;

    @Scheduled(cron = "0 5 0 * * *") // 00:05 every day, server time
    public void aggregateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running nightly revenue aggregation for {}", yesterday);
        revenueAggregationService.aggregate(yesterday);
    }
}
