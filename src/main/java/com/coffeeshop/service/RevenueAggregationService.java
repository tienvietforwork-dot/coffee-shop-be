package com.coffeeshop.service;

import com.coffeeshop.entity.DailyRevenue;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.repository.DailyRevenueRepository;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevenueAggregationService {

    private final OrderRepository orderRepository;
    private final DailyRevenueRepository dailyRevenueRepository;

    /**
     * Aggregates all COMPLETED orders created on the given date into the
     * daily_revenue table (upsert). Used by the nightly scheduled job, but is
     * also safe to call on demand (e.g. to backfill a date).
     */
    @Transactional
    public DailyRevenue aggregate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findCompletedBetween(OrderStatus.COMPLETED, from, to);
        BigDecimal total = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyRevenue revenue = dailyRevenueRepository.findByRevenueDate(date)
                .orElseGet(() -> DailyRevenue.builder().revenueDate(date).build());
        revenue.setTotalRevenue(total);
        revenue.setTotalOrders(orders.size());

        DailyRevenue saved = dailyRevenueRepository.save(revenue);
        log.info("Aggregated daily revenue for {}: {} orders, {} total", date, orders.size(), total);
        return saved;
    }
}
