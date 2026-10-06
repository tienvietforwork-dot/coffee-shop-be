package com.coffeeshop.repository;

import com.coffeeshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    /** [coffeeId, coffeeName, totalQuantity] for completed orders in a period, best sellers first. */
    @Query("""
            select i.coffee.id, i.coffee.name, sum(i.quantity) from OrderItem i
            where i.order.status = com.coffeeshop.entity.enums.OrderStatus.COMPLETED
              and i.order.orderedAt >= :from and i.order.orderedAt < :to
            group by i.coffee.id, i.coffee.name
            order by sum(i.quantity) desc
            """)
    List<Object[]> topCoffees(LocalDateTime from, LocalDateTime to);
}
