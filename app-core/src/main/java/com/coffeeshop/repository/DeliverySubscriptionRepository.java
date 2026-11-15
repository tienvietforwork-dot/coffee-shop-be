package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliverySubscription;
import com.coffeeshop.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliverySubscriptionRepository extends JpaRepository<DeliverySubscription, Long> {
    @EntityGraph(attributePaths = {"customer", "address"})
    List<DeliverySubscription> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"customer", "address"})
    List<DeliverySubscription> findByCustomerIdOrderByIdDesc(Long customerId);

    List<DeliverySubscription> findByStatus(SubscriptionStatus status);

    boolean existsByCode(String code);
}
