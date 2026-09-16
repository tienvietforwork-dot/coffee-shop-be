package com.coffeeshop.service;

import com.coffeeshop.dto.request.ProductMaterialRequest;
import com.coffeeshop.dto.request.ProductRequest;
import com.coffeeshop.dto.response.ProductResponse;
import com.coffeeshop.entity.Category;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductMaterial;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CategoryRepository;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.ProductMaterialRepository;
import com.coffeeshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ProductMaterialRepository productMaterialRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .category(resolveCategory(request.getCategoryId()))
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .active(request.getActive() == null || request.getActive())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        product.setName(request.getName());
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getEntity(id));
    }

    @Transactional
    public ProductResponse setRecipe(Long productId, List<ProductMaterialRequest> items) {
        Product product = getEntity(productId);
        productMaterialRepository.deleteAll(productMaterialRepository.findByProductId(productId));

        List<ProductMaterial> recipe = items.stream().map(item -> {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + item.getMaterialId()));
            return ProductMaterial.builder()
                    .product(product)
                    .material(material)
                    .quantityRequired(item.getQuantityRequired())
                    .build();
        }).collect(Collectors.toList());

        productMaterialRepository.saveAll(recipe);
        product.getRecipe().clear();
        product.getRecipe().addAll(recipe);
        return ProductResponse.from(product);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
