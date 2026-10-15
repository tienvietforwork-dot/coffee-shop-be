package com.coffeeshop.repository;

import com.coffeeshop.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    @Query("select h from OrderStatusHistory h left join fetch h.staff where h.order.id = :orderId order by h.id")
    List<OrderStatusHistory> findByOrder(Long orderId);
}
