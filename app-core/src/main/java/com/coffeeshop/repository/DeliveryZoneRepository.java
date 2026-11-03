package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
    List<DeliveryZone> findAllByOrderBySortOrderAscIdAsc();
}
