package com.coffeeshop.repository;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to")
    List<Order> findCompletedBetween(@Param("status") OrderStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to")
    List<Order> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
