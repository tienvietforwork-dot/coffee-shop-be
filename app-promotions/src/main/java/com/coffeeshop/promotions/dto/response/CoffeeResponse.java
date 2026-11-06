package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.Coffee;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoffeeResponse {
    Long id;
    Long category;
    String name;
    String imageUrl;
    BigDecimal price;
    BigDecimal sale;
    BigDecimal salePrice;
    String description;
    String status;
    Boolean autoSoldOut;

    public static CoffeeResponse from (Coffee coffee){

        BigDecimal price = coffee.getPrice();

        BigDecimal sale = coffee.getSale() != null ? coffee.getSale() : BigDecimal.ZERO;

        BigDecimal salePrice = coffee.getSalePrice() != null ? coffee.getSalePrice() : price;
        return CoffeeResponse.builder()
                .id(coffee.getId())
                .category(coffee.getCategory())
                .name(coffee.getName())
                .imageUrl(coffee.getImageUrl())
                .price(coffee.getPrice())
                .sale(sale)
                .salePrice(salePrice)
                .description(coffee.getDescription())
                .status(coffee.getStatus() != null ? coffee.getStatus().name() : null)
                .autoSoldOut(coffee.isAutoSoldOut())
                .build();
    }
}
