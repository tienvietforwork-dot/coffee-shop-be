package com.coffeeshop.stats.dto.response;

import com.coffeeshop.stats.entity.ProductPlacementRule;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;

import java.time.LocalDateTime;

public record ProductPlacementResponse(Long id, ProductPlacementSection section,
                                       Long targetCategoryId, String targetCategoryName,
                                       String title, Integer displayOrder,
                                       LocalDateTime startsAt, LocalDateTime endsAt, boolean active,
                                       PublicationStatus publicationStatus, boolean visibleNow, String visibilityReason,
                                       CoffeePlacementResponse coffee) {

    public static ProductPlacementResponse from(ProductPlacementRule rule, LocalDateTime now) {
        boolean coffeeAvailable = rule.getCoffee().getStatus() == CoffeeStatus.AVAILABLE && !rule.getCoffee().isAutoSoldOut();
        boolean targetCategoryAvailable = rule.getSection() != ProductPlacementSection.CATEGORY || rule.getTargetCategory() != null;
        boolean visibleNow = rule.isVisibleNow(now) && coffeeAvailable && targetCategoryAvailable;
        String visibilityReason;
        if (!targetCategoryAvailable) {
            visibilityReason = "Danh mục trưng bày không còn tồn tại";
        } else if (rule.getCoffee().isAutoSoldOut()
                || rule.getCoffee().getStatus() == CoffeeStatus.SOLD_OUT) {
            visibilityReason = "Hết hàng";
        } else if (rule.getCoffee().getStatus() == CoffeeStatus.HIDDEN) {
            visibilityReason = "Sản phẩm đang ẩn";
        } else if (rule.getCoffee().getStatus() == CoffeeStatus.DISCONTINUED) {
            visibilityReason = "Sản phẩm đã ngừng bán";
        } else if (rule.getPublicationStatus() == PublicationStatus.DRAFT) {
            visibilityReason = "Bản nháp";
        } else if (!rule.isActive()) {
            visibilityReason = "Cấu hình đang tắt";
        } else if (rule.getStartsAt() != null && rule.getStartsAt().isAfter(now)) {
            visibilityReason = "Chưa đến thời gian bắt đầu";
        } else if (rule.getEndsAt() != null && !now.isBefore(rule.getEndsAt())) {
            visibilityReason = "Lịch trưng bày đã kết thúc";
        } else {
            visibilityReason = "Đang hiển thị";
        }

        return new ProductPlacementResponse(rule.getId(), rule.getSection(),
                rule.getTargetCategory() == null ? null : rule.getTargetCategory().getId(),
                rule.getTargetCategory() == null ? null : rule.getTargetCategory().getName(),
                rule.getTitle(), rule.getDisplayOrder(), rule.getStartsAt(), rule.getEndsAt(), rule.isActive(),
                rule.getPublicationStatus(), visibleNow, visibilityReason, CoffeePlacementResponse.from(rule.getCoffee()));
    }
}
