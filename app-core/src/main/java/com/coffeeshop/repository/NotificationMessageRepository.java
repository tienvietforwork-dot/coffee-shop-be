package com.coffeeshop.repository;

import com.coffeeshop.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    boolean existsByRequestId(String requestId);

    Optional<NotificationMessage> findByRequestId(String requestId);

    @Query("select m from NotificationMessage m left join fetch m.order where m.status = 'PENDING' and m.attempts < :maxAttempts order by m.id")
    List<NotificationMessage> findQueued(int maxAttempts);

    @Query("""
            select m from NotificationMessage m left join fetch m.order o
            where m.createdAt >= :from and m.createdAt < :to and (:status is null or m.status = :status)
              and (:phone is null or m.phone like :phone)
            order by m.createdAt desc, m.id desc
            """)
    List<NotificationMessage> search(LocalDateTime from, LocalDateTime to, String status, String phone);
}
