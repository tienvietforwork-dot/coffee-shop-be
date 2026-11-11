package com.coffeeshop.repository;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderChannel;
import com.coffeeshop.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findFirstBySubscriptionIdOrderByIdDesc(Long subscriptionId);

    long countBySubscriptionId(Long subscriptionId);

    /** lịch hẹn: đơn mang đi / giao còn đang xử lý có giờ hẹn trong [from, to), hoặc không hẹn giờ (làm ngay) */
    @Query("""
            select o from Order o
            where o.orderType in (com.coffeeshop.entity.enums.OrderType.DELIVERY, com.coffeeshop.entity.enums.OrderType.TAKE_AWAY)
              and o.status in :active
              and ((o.pickupTime >= :from and o.pickupTime < :to) or o.pickupTime is null)
            order by o.id""")
    List<Order> findSchedule(java.util.Collection<OrderStatus> active, LocalDateTime from, LocalDateTime to);

    List<Order> findByCustomerIdOrderByOrderedAtDesc(Long customerId);

    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:channel is null or o.channel = :channel)
              and o.orderedAt >= :from and o.orderedAt < :to
            order by o.orderedAt desc
            """)
    List<Order> search(OrderStatus status, OrderChannel channel, LocalDateTime from, LocalDateTime to);

    long countByOrderedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusIn(List<OrderStatus> statuses);

    boolean existsByTableIdAndStatusInAndIdNot(Long tableId, java.util.Collection<OrderStatus> statuses, Long id);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.status = com.coffeeshop.entity.enums.OrderStatus.COMPLETED
              and o.orderedAt >= :from and o.orderedAt < :to
            """)
    BigDecimal revenueBetween(LocalDateTime from, LocalDateTime to);
}
