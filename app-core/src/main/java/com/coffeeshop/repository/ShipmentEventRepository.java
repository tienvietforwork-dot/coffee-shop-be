package com.coffeeshop.repository;

import com.coffeeshop.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    List<ShipmentEvent> findByShipmentIdOrderByCreatedAtAscIdAsc(Long shipmentId);
}
