package com.coffeeshop.stats.repository;

import com.coffeeshop.stats.entity.ProductBundleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBundleItemRepository extends JpaRepository<ProductBundleItem, Long> {

    boolean existsByBundle_IdAndCoffee_Id(Long bundleId, Long coffeeId);

    boolean existsByBundle_IdAndCoffee_IdAndIdNot(Long bundleId, Long coffeeId, Long id);
}
