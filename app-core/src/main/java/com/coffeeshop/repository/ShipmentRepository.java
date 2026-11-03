package com.coffeeshop.repository;

import com.coffeeshop.entity.Shipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @EntityGraph(attributePaths = {"order", "address"})
    List<Shipment> findAllByOrderByIdDesc();

    java.util.Optional<Shipment> findByPartnerOrderId(String partnerOrderId);

    List<Shipment> findByPartnerAndStatusIn(String partner, java.util.Collection<com.coffeeshop.entity.enums.ShipmentStatus> statuses);
}
