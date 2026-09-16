package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductMaterialRequest {

    @NotNull
    private Long materialId;

    @NotNull
    @Positive
    private BigDecimal quantityRequired;
}
