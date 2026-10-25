package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.Coffee;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder

public class CoffeeResponse {
    Long id;
    Long category;
    String name;
    String imageUrl;
    BigDecimal price;
    String description;
    String status;
    Boolean autoSoldOut;

    public static CoffeeResponse from (Coffee coffee){
        return CoffeeResponse.builder()
                .id(coffee.getId())
                .category(coffee.getCategory())
                .name(coffee.getName())
                .imageUrl(coffee.getImageUrl())
                .price(coffee.getPrice())
                .description(coffee.getDescription())
                .status(coffee.getStatus() != null ? coffee.getStatus().name() : null)
                .autoSoldOut(coffee.isAutoSoldOut())
                .build();
    }
}
