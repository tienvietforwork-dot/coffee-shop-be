package com.coffeeshop.repository;

import com.coffeeshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByIdAsc(Long orderId);

    /** collected in a window, per method: [method, count, amount] (payments PAID or later refunded) */
    @Query("""
            select p.method, count(p), sum(p.amount) from Payment p
            where p.paidAt >= :from and p.paidAt < :to
              and p.status in (com.coffeeshop.entity.enums.PaymentStatus.PAID, com.coffeeshop.entity.enums.PaymentStatus.REFUNDED)
            group by p.method
            """)
    List<Object[]> collectedBetween(LocalDateTime from, LocalDateTime to);
}
