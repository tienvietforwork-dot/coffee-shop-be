package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;

    public static CategoryResponse from(Category c) {
        return CategoryResponse.builder().id(c.getId()).name(c.getName()).build();
    }
}
