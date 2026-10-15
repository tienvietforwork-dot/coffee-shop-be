package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.Category;

public record CategoryPlacementResponse(Long id, String name, String description, Integer displayOrder) {

    public static CategoryPlacementResponse from(Category category) {
        return new CategoryPlacementResponse(category.getId(), category.getName(), category.getDescription(),
                category.getDisplayOrder());
    }
}
