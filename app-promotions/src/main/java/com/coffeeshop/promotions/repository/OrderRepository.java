package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COUNT (o) FROM Order o WHERE o.promotionId = :promoId AND o.orderedAt BETWEEN :fromDate AND :toDate")
    long countOrdersByPromotion(@Param("promoId") Long promoId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.promotionId = :promoId AND o.orderedAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumRevenueByPromotion(@Param("promoId") Long promoId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.promotionId = :promoId AND o.orderedAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumDiscountByPromotion(@Param("promoId") Long promoId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
