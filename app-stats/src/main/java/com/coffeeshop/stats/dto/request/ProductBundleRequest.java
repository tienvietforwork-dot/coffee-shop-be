package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.ProductBundleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductBundleRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 10000) String description,
        @Positive Long imageId,
        @Size(max = 500) @Pattern(regexp = "^(https?://\\S+|/api/public/images/\\d+)?$", message = "Đường dẫn ảnh phải là HTTP(S) hoặc ảnh đã tải lên") String imageUrl,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal bundlePrice,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        ProductBundleStatus status,
        @Valid @Size(max = 50) List<ProductBundleItemRequest> items) {
}
