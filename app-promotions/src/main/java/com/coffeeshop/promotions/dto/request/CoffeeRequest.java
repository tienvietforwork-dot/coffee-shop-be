package com.coffeeshop.promotions.dto.request;

import com.coffeeshop.promotions.entity.enums.CoffeeStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CoffeeRequest {

    private Long id;
    private Long category;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal sale;
    private BigDecimal salePrice;
    private String description;
    private CoffeeStatus status;
    private Boolean autoSoldOut;
}
