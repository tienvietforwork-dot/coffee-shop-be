package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.ProductPlacementRule;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;

import java.time.LocalDateTime;

public record ProductPlacementResponse(Long id, ProductPlacementSection section, String title, Integer displayOrder,
                                       LocalDateTime startsAt, LocalDateTime endsAt, boolean active,
                                       PublicationStatus publicationStatus, boolean visibleNow,
                                       CoffeePlacementResponse coffee) {

    public static ProductPlacementResponse from(ProductPlacementRule rule, LocalDateTime now) {
        return new ProductPlacementResponse(rule.getId(), rule.getSection(), rule.getTitle(), rule.getDisplayOrder(),
                rule.getStartsAt(), rule.getEndsAt(), rule.isActive(), rule.getPublicationStatus(),
                rule.isVisibleNow(now), CoffeePlacementResponse.from(rule.getCoffee()));
    }
}
