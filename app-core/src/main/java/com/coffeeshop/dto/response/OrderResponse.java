package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String orderType;
    private String status;
    private BigDecimal totalAmount;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdByUsername(order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()))
                .build();
    }
}
