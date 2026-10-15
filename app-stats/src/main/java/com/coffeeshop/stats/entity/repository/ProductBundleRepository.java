package com.coffeeshop.stats.repository;

import com.coffeeshop.stats.entity.ProductBundle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductBundleRepository extends JpaRepository<ProductBundle, Long> {

    @EntityGraph(attributePaths = {"items", "items.coffee", "items.coffee.category"})
    List<ProductBundle> findAllByOrderByStatusAscNameAsc();

    @EntityGraph(attributePaths = {"items", "items.coffee", "items.coffee.category"})
    Optional<ProductBundle> findById(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
