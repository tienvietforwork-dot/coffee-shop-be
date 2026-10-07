package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.entity.enums.MaterialStatus;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.List;

public record MaterialResponse(Long id, String name, String unit, BigDecimal stockQuantity, BigDecimal minStock,
                               MaterialStatus status, boolean lowStock, MaterialKind kind, BigDecimal yieldQuantity,
                               Integer prepMinutes, Integer shelfLifeMinutes, List<Component> components,
                               String instructions, List<Usage> usedIn) {

    /** where a material goes: a coffee (per cup, kind COFFEE) or a prepared material (per standard batch, kind PREPARED) */
    public record Usage(String kind, Long id, String name, BigDecimal quantity) {
    }

    public record Component(Long componentId, String name, String unit, BigDecimal quantity) {
    }

    public static MaterialResponse from(Material m) {
        return from(m, null);
    }

    public static MaterialResponse from(Material m, List<Usage> usedIn) {
        // the formula is only sent where it was loaded (alerts built outside a transaction skip it)
        List<Component> components = Hibernate.isInitialized(m.getComponents())
                ? m.getComponents().stream().map(c -> new Component(c.getComponent().getId(), c.getComponent().getName(),
                c.getComponent().getUnit(), c.getQuantity())).toList()
                : null;
        return new MaterialResponse(m.getId(), m.getName(), m.getUnit(), m.getStockQuantity(), m.getMinStock(),
                m.getStatus(), m.isLowStock(), m.getKind(), m.getYieldQuantity(), m.getPrepMinutes(), m.getShelfLifeMinutes(),
                components, m.getInstructions(), usedIn);
    }
}
