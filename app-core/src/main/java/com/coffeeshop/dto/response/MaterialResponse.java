package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Material;

import java.math.BigDecimal;
import java.util.List;

/** {@code usedIn}: coffees whose active recipe uses the material, with the quantity per cup (only on the list). */
public record MaterialResponse(Long id, String name, String unit, List<Usage> usedIn) {

    /** kind is always "COFFEE" */
    public record Usage(String kind, Long id, String name, BigDecimal quantity) {
    }

    public static MaterialResponse from(Material m) {
        return from(m, null);
    }

    public static MaterialResponse from(Material m, List<Usage> usedIn) {
        return new MaterialResponse(m.getId(), m.getName(), m.getUnit(), usedIn);
    }
}
