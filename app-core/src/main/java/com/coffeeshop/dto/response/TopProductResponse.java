package com.coffeeshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class TopProductResponse {
    private Long productId;
    private String productName;
    private long totalQuantity;
    private BigDecimal totalRevenue;
}
