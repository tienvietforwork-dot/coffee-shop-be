package com.coffeeshop.stats.entity.repository;

import com.coffeeshop.stats.entity.ProductPlacementRule;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPlacementRuleRepository extends JpaRepository<ProductPlacementRule, Long> {

    @EntityGraph(attributePaths = {"coffee", "coffee.category"})
    List<ProductPlacementRule> findAllByOrderBySectionAscDisplayOrderAscIdAsc();

    boolean existsByCoffee_IdAndSection(Long coffeeId, ProductPlacementSection section);

    boolean existsByCoffee_IdAndSectionAndIdNot(Long coffeeId, ProductPlacementSection section, Long id);
}
