package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Recipe;

import java.math.BigDecimal;
import java.util.List;

public record RecipeResponse(Long id, Long coffeeId, String coffeeName, String brewMethod, Integer version,
                             String description, Integer brewTimeMin, boolean active,
                             List<MaterialLine> materials, List<Step> steps) {

    public record MaterialLine(Long materialId, String materialName, String unit, BigDecimal quantity, String note) {
    }

    public record Step(Integer stepNo, String instruction) {
    }

    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(r.getId(), r.getCoffee().getId(), r.getCoffee().getName(), r.getBrewMethod(),
                r.getVersion(), r.getDescription(), r.getBrewTimeMin(), r.isActive(),
                r.getMaterials().stream().map(m -> new MaterialLine(m.getMaterial().getId(), m.getMaterial().getName(),
                        m.getMaterial().getUnit(), m.getQuantity(), m.getNote())).toList(),
                r.getSteps().stream().map(s -> new Step(s.getStepNo(), s.getInstruction())).toList());
    }
}
