package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn giao. {@code shippingFee}: what the customer paid (phí ship theo khu vực); {@code partnerFee}: what the shop pays
 * the partner (Ahamove) for the trip.
 */
public record ShipmentResponse(Long id, Long orderId, String orderCode, OrderStatus orderStatus, String recipientName,
                               String recipientPhone, String address, String district, String zoneName, String carrier,
                               String trackingCode, BigDecimal shippingFee, ShipmentStatus status, LocalDateTime bookedAt,
                               LocalDateTime driverAcceptedAt, LocalDateTime deliveredAt, String note, String partner,
                               String partnerOrderId, String partnerStatus, BigDecimal partnerFee, String trackingUrl,
                               String driverName, String driverPhone, BigDecimal distanceKm) {
    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(s.getId(), s.getOrder().getId(), s.getOrder().getOrderCode(), s.getOrder().getStatus(),
                s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), s.getAddress().getAddress(),
                s.getDistrict(), s.getZoneName(), s.getCarrier(), s.getTrackingCode(), s.getShippingFee(), s.getStatus(),
                s.getBookedAt(), s.getDriverAcceptedAt(), s.getDeliveredAt(), s.getNote(), s.getPartner(),
                s.getPartnerOrderId(), s.getPartnerStatus(), s.getPartnerFee(), s.getTrackingUrl(), s.getDriverName(),
                s.getDriverPhone(), s.getDistanceKm());
    }

    /** one line of the trip's history */
    public record Event(Long id, String source, ShipmentStatus status, String partnerStatus, String note, String by,
                       LocalDateTime at) {
        public static Event from(ShipmentEvent e) {
            return new Event(e.getId(), e.getSource(), e.getStatus(), e.getPartnerStatus(), e.getNote(), e.getCreatedBy(),
                    e.getCreatedAt());
        }
    }
}
