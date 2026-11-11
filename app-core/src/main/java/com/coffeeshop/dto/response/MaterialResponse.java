package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Material;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code usedIn}: coffees whose active recipe uses the material, with the quantity per cup (only on the list).
 * {@code cupsLeft}: số ly pha được ít nhất trong các món dùng nguyên liệu này (null khi không món nào dùng).
 */
public record MaterialResponse(Long id, String name, String unit, BigDecimal quantity, Integer cupsLeft, List<Usage> usedIn) {

    /** kind is always "COFFEE" */
    public record Usage(String kind, Long id, String name, BigDecimal quantity) {
    }

    public static MaterialResponse from(Material m) {
        return from(m, null);
    }

    public static MaterialResponse from(Material m, List<Usage> usedIn) {
        Integer cups = null;
        if (usedIn != null) {
            for (Usage u : usedIn) {
                if (u.quantity() == null || u.quantity().signum() <= 0) continue;
                int n = m.getQuantity().signum() <= 0 ? 0 : m.getQuantity().divideToIntegralValue(u.quantity()).intValue();
                cups = cups == null ? n : Math.min(cups, n);
            }
        }
        return new MaterialResponse(m.getId(), m.getName(), m.getUnit(), m.getQuantity(), cups, usedIn);
    }
}
