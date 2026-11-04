package com.coffeeshop.repository;

import com.coffeeshop.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    /** used by any recipe version (active or not) */
    @Query("select count(rm) > 0 from RecipeMaterial rm where rm.material.id = :materialId")
    boolean isUsedInRecipe(Long materialId);
}
