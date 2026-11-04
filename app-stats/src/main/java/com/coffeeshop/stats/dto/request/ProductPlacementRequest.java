package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProductPlacementRequest(
        @NotNull(message = "Chọn sản phẩm cần trưng bày") Long coffeeId,
        @NotNull(message = "Chọn khu vực trưng bày") ProductPlacementSection section,
        @Size(max = 150, message = "Tiêu đề không được quá 150 ký tự") String title,
        @Min(value = 0, message = "Thứ tự ưu tiên phải là số nguyên không âm") Integer displayOrder,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        PublicationStatus publicationStatus) {
}
