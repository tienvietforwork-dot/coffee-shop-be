package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Category;
import com.coffeeshop.entity.enums.CategoryProductType;

public record CategoryResponse(Long id, String name, String description, CategoryProductType productType, Integer displayOrder) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getProductType(), c.getDisplayOrder());
    }
}
