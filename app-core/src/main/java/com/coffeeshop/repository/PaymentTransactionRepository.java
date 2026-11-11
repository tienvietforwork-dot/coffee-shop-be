package com.coffeeshop.repository;

import com.coffeeshop.entity.PaymentTransaction;
import com.coffeeshop.entity.enums.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    List<PaymentTransaction> findByPaymentIdOrderByIdDesc(Long paymentId);

    @Query("""
            select t from PaymentTransaction t join fetch t.payment p join fetch p.order o
            where t.createdAt >= :from and t.createdAt < :to and (:status is null or t.status = :status)
            order by t.createdAt desc, t.id desc
            """)
    List<PaymentTransaction> search(LocalDateTime from, LocalDateTime to, PaymentTransactionStatus status);

    @Query("select t from PaymentTransaction t join fetch t.payment p join fetch p.order where t.status = com.coffeeshop.entity.enums.PaymentTransactionStatus.PENDING and t.type = 'PAY' and t.expireAt < :now")
    List<PaymentTransaction> findExpired(LocalDateTime now);

    /**
     * QR / online orders still waiting for an online payment, placed before {@code orderedBefore}, with no VNPAY link
     * still open: the customer never opened the link, or the attempt failed / was cancelled on VNPAY.
     */
    @Query("""
            select distinct o.id from Payment p join p.order o
            where o.status = com.coffeeshop.entity.enums.OrderStatus.PENDING
              and o.channel <> com.coffeeshop.entity.enums.OrderChannel.COUNTER
              and o.orderedAt < :orderedBefore
              and p.status = com.coffeeshop.entity.enums.PaymentStatus.PENDING
              and p.method <> com.coffeeshop.entity.enums.PaymentMethod.CASH
              and not exists (select t.id from PaymentTransaction t where t.payment = p
                              and t.status = com.coffeeshop.entity.enums.PaymentTransactionStatus.PENDING
                              and t.expireAt >= :now)
            """)
    List<Long> findUnpaidOnlineOrders(LocalDateTime orderedBefore, LocalDateTime now);
}
