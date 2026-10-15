package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.entity.enums.MaterialStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** For a PREPARED material: yieldQuantity + components = the formula of one batch. */
public record MaterialRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String unit,
        @DecimalMin("0") BigDecimal minStock,
        @Min(0) Integer expiryWarningDays,
        MaterialStatus status,
        MaterialKind kind,
        @DecimalMin("0.01") BigDecimal yieldQuantity,
        @Min(1) Integer shelfLifeMinutes,
        @Valid List<Component> components,
        /** raw materials: how they are bought; replaces the current list */
        @Valid List<Package> packages,
        /** how to make one standard batch */
        String instructions) {

    public record Package(@NotBlank @Size(max = 50) String name, @NotNull @DecimalMin("0.01") BigDecimal size) {
    }

    public record Component(@NotNull Long componentId, @NotNull @DecimalMin("0.01") BigDecimal quantity) {
    }
}
