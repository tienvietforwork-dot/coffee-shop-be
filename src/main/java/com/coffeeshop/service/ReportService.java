package com.coffeeshop.service;

import com.coffeeshop.dto.response.RevenueReportResponse;
import com.coffeeshop.dto.response.TopProductResponse;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.repository.OrderItemRepository;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Aggregates COMPLETED orders directly (works for any range, including "today"
     * which the nightly daily_revenue job has not aggregated yet).
     */
    public RevenueReportResponse getRevenue(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        List<Order> completedOrders = orderRepository.findCompletedBetween(OrderStatus.COMPLETED, fromDateTime, toDateTime);

        Map<LocalDate, RevenueReportResponse.DailyPoint> byDate = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : completedOrders) {
            LocalDate date = order.getCreatedAt().toLocalDate();
            RevenueReportResponse.DailyPoint point = byDate.get(date);
            BigDecimal amount = order.getTotalAmount();
            total = total.add(amount);
            if (point == null) {
                byDate.put(date, RevenueReportResponse.DailyPoint.builder()
                        .date(date).revenue(amount).orders(1).build());
            } else {
                byDate.put(date, RevenueReportResponse.DailyPoint.builder()
                        .date(date)
                        .revenue(point.getRevenue().add(amount))
                        .orders(point.getOrders() + 1)
                        .build());
            }
        }

        return RevenueReportResponse.builder()
                .from(from)
                .to(to)
                .totalRevenue(total)
                .totalOrders(completedOrders.size())
                .daily(byDate.values().stream()
                        .sorted(Comparator.comparing(RevenueReportResponse.DailyPoint::getDate))
                        .collect(Collectors.toList()))
                .build();
    }

    public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        return orderItemRepository.findTopProducts(fromDateTime, toDateTime, PageRequest.of(0, limit))
                .stream()
                .map(row -> TopProductResponse.builder()
                        .productId(row.getProductId())
                        .productName(row.getProductName())
                        .totalQuantity(row.getTotalQuantity())
                        .totalRevenue(row.getTotalRevenue())
                        .build())
                .collect(Collectors.toList());
    }
}
