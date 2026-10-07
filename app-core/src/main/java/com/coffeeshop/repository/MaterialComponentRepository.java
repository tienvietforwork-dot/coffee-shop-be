package com.coffeeshop.repository;

import com.coffeeshop.entity.MaterialComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialComponentRepository extends JpaRepository<MaterialComponent, Long> {
    /** every formula line with both ends loaded — for "used in" on the materials list */
    @Query("select c from MaterialComponent c join fetch c.material join fetch c.component")
    List<MaterialComponent> findAllWithMaterials();
}
