package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Material;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class MaterialResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal quantityInStock;
    private BigDecimal minThreshold;
    private BigDecimal unitPrice;
    private boolean belowThreshold;

    public static MaterialResponse from(Material m) {
        return MaterialResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .unit(m.getUnit())
                .quantityInStock(m.getQuantityInStock())
                .minThreshold(m.getMinThreshold())
                .unitPrice(m.getUnitPrice())
                .belowThreshold(m.isBelowThreshold())
                .build();
    }
}
