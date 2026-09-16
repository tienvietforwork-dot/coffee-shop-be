package com.coffeeshop.repository;

import com.coffeeshop.entity.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, ProductMaterial.ProductMaterialId> {
    List<ProductMaterial> findByProductId(Long productId);
}
