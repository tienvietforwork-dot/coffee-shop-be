package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.enums.MaterialStatus;

import java.math.BigDecimal;

public record MaterialResponse(Long id, String name, String unit, BigDecimal stockQuantity, BigDecimal minStock,
                               MaterialStatus status, boolean lowStock) {
    public static MaterialResponse from(Material m) {
        return new MaterialResponse(m.getId(), m.getName(), m.getUnit(), m.getStockQuantity(), m.getMinStock(),
                m.getStatus(), m.isLowStock());
    }
}
