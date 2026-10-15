package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.ProductBundle;
import com.coffeeshop.stats.entity.enums.ProductBundleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductBundleResponse(Long id, String name, String description, BigDecimal bundlePrice,
                                    BigDecimal standalonePrice, BigDecimal savingAmount,
                                    LocalDateTime startsAt, LocalDateTime endsAt, ProductBundleStatus status,
                                    boolean availableNow, List<ProductBundleItemResponse> items) {

    public static ProductBundleResponse from(ProductBundle bundle, LocalDateTime now) {
        List<ProductBundleItemResponse> items = bundle.getItems().stream()
                .map(ProductBundleItemResponse::from)
                .toList();
        BigDecimal standalonePrice = items.stream()
                .map(ProductBundleItemResponse::linePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savingAmount = standalonePrice.subtract(bundle.getBundlePrice());
        return new ProductBundleResponse(bundle.getId(), bundle.getName(), bundle.getDescription(),
                bundle.getBundlePrice(), standalonePrice, savingAmount, bundle.getStartsAt(), bundle.getEndsAt(),
                bundle.getStatus(), bundle.isAvailableNow(now), items);
    }
}
