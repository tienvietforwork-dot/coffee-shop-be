package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** A combo (product_bundles) on an order taken by staff: which combo and how many. */
public record BundleLineRequest(@NotNull Long bundleId, @NotNull @Min(1) @Max(99) Integer quantity) {
}
