package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    private String name;

    private Long categoryId;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    private String imageUrl;

    private String description;

    private Boolean active;
}
