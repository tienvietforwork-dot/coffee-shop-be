package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentResponse(Long id, Long orderId, String orderCode, String recipientName, String recipientPhone,
                               String address, String carrier, String trackingCode, BigDecimal shippingFee,
                               ShipmentStatus status, LocalDateTime bookedAt, LocalDateTime driverAcceptedAt,
                               LocalDateTime deliveredAt, String note) {
    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(s.getId(), s.getOrder().getId(), s.getOrder().getOrderCode(),
                s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), s.getAddress().getAddress(),
                s.getCarrier(), s.getTrackingCode(), s.getShippingFee(), s.getStatus(), s.getBookedAt(),
                s.getDriverAcceptedAt(), s.getDeliveredAt(), s.getNote());
    }
}
