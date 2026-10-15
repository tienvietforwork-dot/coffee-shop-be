package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.ProductBundleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductBundleRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal bundlePrice,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        ProductBundleStatus status,
        @Valid List<ProductBundleItemRequest> items) {
}
