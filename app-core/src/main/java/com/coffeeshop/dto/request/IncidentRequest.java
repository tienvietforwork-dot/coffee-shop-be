package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.IncidentSeverity;
import com.coffeeshop.entity.enums.IncidentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
        @NotNull IncidentType type,
        IncidentSeverity severity,
        @Size(max = 255) String reason,
        String description) {
}
