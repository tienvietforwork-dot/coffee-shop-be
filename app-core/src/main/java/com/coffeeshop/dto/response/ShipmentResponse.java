package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Shipment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private Long orderId;
    private String orderCode;
    private Long shipperId;
    private String shipperName;
    private String address;
    private String status;
    private String note;
    private LocalDateTime deliveredAt;

    public static ShipmentResponse from(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .orderId(s.getOrder().getId())
                .orderCode(s.getOrder().getOrderCode())
                .shipperId(s.getShipper() != null ? s.getShipper().getId() : null)
                .shipperName(s.getShipper() != null ? s.getShipper().getFullName() : null)
                .address(s.getAddress())
                .status(s.getStatus().name())
                .note(s.getNote())
                .deliveredAt(s.getDeliveredAt())
                .build();
    }
}
