package com.coffeeshop.repository;

import com.coffeeshop.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("select m from Material m where m.status = com.coffeeshop.entity.enums.MaterialStatus.ACTIVE and m.stockQuantity < m.minStock order by m.name")
    List<Material> findLowStock();
}
