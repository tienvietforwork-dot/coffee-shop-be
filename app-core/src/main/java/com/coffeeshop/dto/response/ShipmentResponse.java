package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Đơn giao (quán tự giao): phí ship khách trả theo {@code distanceKm} / bậc {@code feeTier}, trạng thái giao. */
public record ShipmentResponse(Long id, Long orderId, String orderCode, OrderStatus orderStatus, String recipientName,
                               String recipientPhone, String address, BigDecimal distanceKm, String feeTier,
                               BigDecimal shippingFee, ShipmentStatus status, LocalDateTime deliveredAt, String note) {
    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(s.getId(), s.getOrder().getId(), s.getOrder().getOrderCode(), s.getOrder().getStatus(),
                s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), s.getAddress().getAddress(),
                s.getDistanceKm(), s.getZoneName(), s.getShippingFee(), s.getStatus(), s.getDeliveredAt(), s.getNote());
    }

    /** one line of the delivery's history */
    public record Event(Long id, String source, ShipmentStatus status, String note, String by, LocalDateTime at) {
        public static Event from(ShipmentEvent e) {
            return new Event(e.getId(), e.getSource(), e.getStatus(), e.getNote(), e.getCreatedBy(), e.getCreatedAt());
        }
    }
}
