package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductMaterial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private boolean active;
    private List<RecipeItem> recipe;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecipeItem {
        private Long materialId;
        private String materialName;
        private BigDecimal quantityRequired;
        private String unit;
    }

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .description(p.getDescription())
                .active(p.isActive())
                .recipe(p.getRecipe() == null ? List.of() : p.getRecipe().stream()
                        .map(ProductResponse::toRecipeItem)
                        .collect(Collectors.toList()))
                .build();
    }

    private static RecipeItem toRecipeItem(ProductMaterial pm) {
        return RecipeItem.builder()
                .materialId(pm.getMaterial().getId())
                .materialName(pm.getMaterial().getName())
                .quantityRequired(pm.getQuantityRequired())
                .unit(pm.getMaterial().getUnit())
                .build();
    }
}
