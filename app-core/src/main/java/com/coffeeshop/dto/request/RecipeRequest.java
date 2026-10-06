package com.coffeeshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequest(
        @Size(max = 30) String brewMethod,
        String description,
        Integer brewTimeMin,
        /** true = make this the recipe currently in use for the coffee. */
        Boolean activate,
        @Valid List<Line> materials,
        List<String> steps) {

    public record Line(@NotNull Long materialId, @NotNull @DecimalMin("0.01") BigDecimal quantity, String note) {
    }
}
