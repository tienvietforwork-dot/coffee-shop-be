package com.coffeeshop.stats.entity.repository;

import com.coffeeshop.stats.entity.ProductPlacementRule;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPlacementRuleRepository extends JpaRepository<ProductPlacementRule, Long> {

    @EntityGraph(attributePaths = {"coffee", "coffee.category", "targetCategory"})
    List<ProductPlacementRule> findAllByOrderBySectionAscDisplayOrderAscIdAsc();

    boolean existsByCoffee_IdAndSectionAndTargetCategory_Id(Long coffeeId, ProductPlacementSection section, Long targetCategoryId);

    boolean existsByCoffee_IdAndSectionAndTargetCategory_IdAndIdNot(Long coffeeId, ProductPlacementSection section,
                                                                    Long targetCategoryId, Long id);

    boolean existsByCoffee_IdAndSectionAndTargetCategoryIsNull(Long coffeeId, ProductPlacementSection section);

    boolean existsByCoffee_IdAndSectionAndTargetCategoryIsNullAndIdNot(Long coffeeId, ProductPlacementSection section,
                                                                       Long id);
}
