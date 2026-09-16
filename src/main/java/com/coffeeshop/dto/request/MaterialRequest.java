package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String unit;

    @NotNull
    @PositiveOrZero
    private BigDecimal quantityInStock;

    @NotNull
    @PositiveOrZero
    private BigDecimal minThreshold;

    @NotNull
    @PositiveOrZero
    private BigDecimal unitPrice;
}
