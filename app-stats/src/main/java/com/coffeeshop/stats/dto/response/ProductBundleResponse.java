package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.ProductBundle;
import com.coffeeshop.stats.entity.enums.ProductBundleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ProductBundleResponse(Long id, String name, String description, Long imageId, String imageUrl,
                                    BigDecimal bundlePrice,
                                    BigDecimal standalonePrice, BigDecimal savingAmount,
                                    LocalDateTime startsAt, LocalDateTime endsAt, ProductBundleStatus status,
                                    boolean availableNow, List<ProductBundleItemResponse> items) {

    public static ProductBundleResponse from(ProductBundle bundle, LocalDateTime now) {
        List<ProductBundleItemResponse> items = bundle.getItems().stream()
                .map(ProductBundleItemResponse::from)
                .toList();
        BigDecimal standalonePrice = items.stream()
                .filter(item -> !item.optional())
                .map(ProductBundleItemResponse::linePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> cheapestChoiceByGroup = new HashMap<>();
        for (ProductBundleItemResponse item : items) {
            if (item.choiceGroup() != null && !item.choiceGroup().isBlank()
                    && item.coffee().status() == com.coffeeshop.stats.entity.enums.CoffeeStatus.AVAILABLE
                    && !item.coffee().autoSoldOut()) {
                cheapestChoiceByGroup.merge(item.choiceGroup().trim(), item.linePrice(), BigDecimal::min);
            }
        }
        standalonePrice = standalonePrice.add(cheapestChoiceByGroup.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal savingAmount = standalonePrice.subtract(bundle.getBundlePrice());
        return new ProductBundleResponse(bundle.getId(), bundle.getName(), bundle.getDescription(),
                bundle.getImageId(), bundle.imageSrc(),
                bundle.getBundlePrice(), standalonePrice, savingAmount, bundle.getStartsAt(), bundle.getEndsAt(),
                bundle.getStatus(), bundle.isAvailableNow(now), items);
    }
}
