package com.coffeeshop.service;

import com.coffeeshop.dto.response.DashboardResponse;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.OrderItemRepository;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Operational overview for the admin home page (detailed analytics live in app-stats). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        var top = orderItemRepository.topCoffees(today.minusDays(6), tomorrow).stream().limit(5)
                .map(r -> new DashboardResponse.TopCoffee((Long) r[0], (String) r[1], ((Number) r[2]).longValue()))
                .toList();
        return new DashboardResponse(
                orderRepository.countByOrderedAtBetween(today, tomorrow),
                orderRepository.revenueBetween(today, tomorrow),
                orderRepository.countByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
                        OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.DELIVERING)),
                materialRepository.findLowStock().size(),
                batchRepository.findExpiringBefore(LocalDate.now().plusDays(7)).size(),
                top);
    }
}
