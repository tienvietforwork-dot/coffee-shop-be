package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliveryFeePlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryFeePlanRepository extends JpaRepository<DeliveryFeePlan, Long> {
    @EntityGraph(attributePaths = "tiers")
    List<DeliveryFeePlan> findAllByOrderByDefaultPlanDescIdAsc();

    @EntityGraph(attributePaths = "tiers")
    Optional<DeliveryFeePlan> findFirstByDefaultPlanTrue();
}
