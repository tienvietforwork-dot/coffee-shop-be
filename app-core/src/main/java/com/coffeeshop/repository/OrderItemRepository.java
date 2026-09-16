package com.coffeeshop.repository;

import com.coffeeshop.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT oi.product.id AS productId, oi.product.name AS productName,
                   SUM(oi.quantity) AS totalQuantity, SUM(oi.subtotal) AS totalRevenue
            FROM OrderItem oi
            WHERE oi.order.status = com.coffeeshop.entity.enums.OrderStatus.COMPLETED
              AND oi.order.createdAt >= :from AND oi.order.createdAt < :to
            GROUP BY oi.product.id, oi.product.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<TopProductRow> findTopProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    interface TopProductRow {
        Long getProductId();
        String getProductName();
        Long getTotalQuantity();
        java.math.BigDecimal getTotalRevenue();
    }
}
