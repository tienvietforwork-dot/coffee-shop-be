package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Category;

public record CategoryResponse(Long id, String name, String description, Integer displayOrder) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getDisplayOrder());
    }
}
