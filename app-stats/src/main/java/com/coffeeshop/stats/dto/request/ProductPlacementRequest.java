package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProductPlacementRequest(
        @NotNull(message = "Chọn sản phẩm cần trưng bày") @Positive(message = "Mã sản phẩm không hợp lệ") Long coffeeId,
        @NotNull(message = "Chọn khu vực trưng bày") ProductPlacementSection section,
        @Positive(message = "Mã danh mục không hợp lệ") Long targetCategoryId,
        @Size(max = 150, message = "Tiêu đề không được quá 150 ký tự") String title,
        @Min(value = 0, message = "Thứ tự ưu tiên phải là số nguyên không âm")
        @Max(value = 2147483647, message = "Thứ tự ưu tiên vượt quá giới hạn") Integer displayOrder,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        PublicationStatus publicationStatus) {
}
