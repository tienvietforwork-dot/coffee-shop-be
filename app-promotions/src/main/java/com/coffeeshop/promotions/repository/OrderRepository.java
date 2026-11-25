package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.customerId = :customerId
              AND o.status = 'COMPLETED'
              AND o.orderedAt >= :startDate
            """)
    List<Order> findCompletedOrdersSince(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT COUNT (o) FROM Order o WHERE o.promotionId = :promoId AND o.orderedAt BETWEEN :fromDate AND :toDate")
    long countOrdersByPromotion(@Param("promoId") Long promoId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.promotionId = :promoId AND o.orderedAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumRevenueByPromotion(@Param("promoId") Long promoId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);





    @Query("""
    SELECT COALESCE(SUM(o.discountAmount), 0)
    FROM Order o
    WHERE o.promotionId = :promotionId
      AND o.orderedAt BETWEEN :fromDate AND :toDate
    """)
    BigDecimal sumDiscountAmountByPromotion(
            @Param("promotionId") Long promotionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
    SELECT COALESCE(SUM(o.voucherDiscount), 0)
    FROM Order o
    JOIN VoucherDetail vd ON o.voucherCode = vd.code
    JOIN vd.voucher v
    WHERE o.orderedAt BETWEEN :fromDate AND :toDate
      AND o.voucherCode IS NOT NULL
      AND (o.delFlag = false OR o.delFlag IS NULL)
      AND (vd.delFlag = false OR vd.delFlag IS NULL)
      AND (v.delFlag = false OR v.delFlag IS NULL)
    """)
    BigDecimal sumVoucherDiscount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
    SELECT COALESCE(AVG(o.totalAmount), 0)
    FROM Order o
    JOIN VoucherDetail vd ON o.voucherCode = vd.code
    JOIN vd.voucher v
    WHERE o.orderedAt BETWEEN :fromDate AND :toDate
      AND o.voucherCode IS NOT NULL
      AND (o.delFlag = false OR o.delFlag IS NULL)
      AND (vd.delFlag = false OR vd.delFlag IS NULL)
      AND (v.delFlag = false OR v.delFlag IS NULL)
    """)
    BigDecimal avgOrderValueWithVoucher(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


}
