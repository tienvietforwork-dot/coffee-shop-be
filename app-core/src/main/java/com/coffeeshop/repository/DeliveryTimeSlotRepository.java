package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliveryTimeSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryTimeSlotRepository extends JpaRepository<DeliveryTimeSlot, Long> {
    @EntityGraph(attributePaths = {"plan", "plan.tiers"})
    List<DeliveryTimeSlot> findAllByOrderByStartTimeAsc();
}
