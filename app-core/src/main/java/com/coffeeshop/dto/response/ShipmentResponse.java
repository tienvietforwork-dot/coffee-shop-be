package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn giao (quán tự giao): khu vực tính phí {@code zone}, người giao được phân công, giờ hẹn giao của đơn
 * ({@code pickupTime}), trạng thái giao.
 */
public record ShipmentResponse(Long id, Long orderId, String orderCode, OrderStatus orderStatus, String recipientName,
                               String recipientPhone, String address, BigDecimal distanceKm, String zone,
                               BigDecimal shippingFee, ShipmentStatus status, LocalDateTime deliveredAt, String note,
                               Long shipperId, String shipperName, String shipperPhone, LocalDateTime assignedAt,
                               LocalDateTime pickedUpAt, LocalDateTime pickupTime, BigDecimal orderTotal, boolean paid,
                               String orderNote) {
    public static ShipmentResponse from(Shipment s) {
        var o = s.getOrder();
        var shipper = s.getShipper();
        boolean paid = !o.getPayments().isEmpty() && o.getPayments().stream()
                .allMatch(p -> p.getStatus() == com.coffeeshop.entity.enums.PaymentStatus.PAID);
        return new ShipmentResponse(s.getId(), o.getId(), o.getOrderCode(), o.getStatus(),
                s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), s.getAddress().getAddress(),
                s.getDistanceKm(), s.getZoneName(), s.getShippingFee(), s.getStatus(), s.getDeliveredAt(), s.getNote(),
                shipper == null ? null : shipper.getId(), shipper == null ? null : shipper.getFullName(),
                shipper == null ? null : shipper.getPhone(), s.getAssignedAt(), s.getPickedUpAt(), o.getPickupTime(),
                o.getTotalAmount(), paid, o.getNote());
    }

    /** one line of the delivery's history */
    public record Event(Long id, String source, ShipmentStatus status, String note, String by, LocalDateTime at) {
        public static Event from(ShipmentEvent e) {
            return new Event(e.getId(), e.getSource(), e.getStatus(), e.getNote(), e.getCreatedBy(), e.getCreatedAt());
        }
    }
}
