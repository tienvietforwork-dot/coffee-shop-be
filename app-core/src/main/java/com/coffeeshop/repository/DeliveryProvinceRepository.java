package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliveryProvince;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryProvinceRepository extends JpaRepository<DeliveryProvince, Long> {
    @EntityGraph(attributePaths = "wards")
    List<DeliveryProvince> findAllByOrderByNameAsc();
}
