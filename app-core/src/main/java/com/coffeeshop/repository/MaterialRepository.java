package com.coffeeshop.repository;

import com.coffeeshop.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Query("SELECT m FROM Material m WHERE m.quantityInStock < m.minThreshold")
    List<Material> findLowStock();
}
