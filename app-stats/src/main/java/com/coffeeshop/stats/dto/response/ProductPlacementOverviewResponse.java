package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.enums.ProductPlacementSection;

import java.util.List;
import java.util.Map;

public record ProductPlacementOverviewResponse(Map<ProductPlacementSection, List<ProductPlacementResponse>> sections,
                                               List<CategoryPlacementResponse> categories,
                                               List<CoffeePlacementResponse> coffees) {
}
