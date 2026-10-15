package com.coffeeshop.stats.dto.request;

import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProductPlacementRequest(
        @NotNull Long coffeeId,
        @NotNull ProductPlacementSection section,
        @Size(max = 150) String title,
        @Min(0) Integer displayOrder,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        PublicationStatus publicationStatus) {
}
