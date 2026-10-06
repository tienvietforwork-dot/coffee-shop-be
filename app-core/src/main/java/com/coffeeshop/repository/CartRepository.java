package com.coffeeshop.repository;

import com.coffeeshop.entity.Cart;
import com.coffeeshop.entity.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findBySessionCode(String sessionCode);

    @Modifying
    @Query("update Cart c set c.status = :to, c.updatedBy = 'system', c.updatedAt = CURRENT_TIMESTAMP "
            + "where c.status = :from and c.createdAt < :before")
    int markStale(CartStatus from, CartStatus to, LocalDateTime before);
}
