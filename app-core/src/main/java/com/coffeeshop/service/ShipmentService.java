package com.coffeeshop.service;

import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.Shipment;
import com.coffeeshop.entity.ShipmentEvent;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.entity.enums.ShipmentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.ahamove.AhamoveGateway;
import com.coffeeshop.repository.ShipmentEventRepository;
import com.coffeeshop.repository.ShipmentRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.websocket.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Điều phối giao hàng: đặt xe → tài xế nhận → đang giao → đã giao. Two ways, as in CukCuk / Sapo "sổ giao hàng":
 * <ul>
 *   <li>qua Ahamove: báo giá → đặt xe (API) → the partner's webhook moves the trip; hủy chuyến while no one picked up;</li>
 *   <li>gọi xe ngoài (Grab, Be, shipper quán): staff types the carrier / mã vận đơn and updates the status by hand.</li>
 * </ul>
 * Every change is kept in shipment_events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    public static final String AHAMOVE = "AHAMOVE";

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final AhamoveGateway ahamove;

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        return shipmentRepository.findAllByOrderByIdDesc().stream().map(ShipmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse.Event> events(Long id) {
        get(id);
        return eventRepository.findByShipmentIdOrderByCreatedAtAscIdAsc(id).stream().map(ShipmentResponse.Event::from).toList();
    }

    /** Gọi xe ngoài: carrier / mã vận đơn typed by staff, status moved by hand. */
    @Transactional
    public ShipmentResponse update(Long id, ShipmentUpdateRequest request) {
        Shipment shipment = get(id);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.FAILED) {
            throw new BadRequestException("Đơn giao đã kết thúc");
        }
        if (AHAMOVE.equals(shipment.getPartner()) && isActive(shipment)) {
            throw new BadRequestException("Chuyến đang chạy qua Ahamove — trạng thái tự cập nhật; hủy chuyến nếu muốn gọi xe khác");
        }
        if (request.carrier() != null) shipment.setCarrier(request.carrier());
        if (request.trackingCode() != null) shipment.setTrackingCode(request.trackingCode());
        if (request.note() != null) shipment.setNote(request.note());
        if (request.status() == ShipmentStatus.BOOKED && (shipment.getCarrier() == null || shipment.getCarrier().isBlank())) {
            throw new BadRequestException("Cần nhập đơn vị vận chuyển khi đặt xe");
        }
        boolean leaving = request.status() == ShipmentStatus.DELIVERING || request.status() == ShipmentStatus.DELIVERED;
        if (leaving && shipment.getOrder().getStatus() == OrderStatus.READY) {
            move(shipment, ShipmentStatus.DELIVERING, "STAFF", null, request.note(), null);
            orderService.dispatch(shipment.getOrder().getId()); // tài xế đã lấy hàng
        }
        move(shipment, request.status(), "STAFF", null, request.note(), null);
        return ShipmentResponse.from(shipment);
    }

    // ---- Ahamove -------------------------------------------------------------

    /** Báo giá Ahamove for the trip shop → customer. */
    @Transactional(readOnly = true)
    public AhamoveGateway.Estimate estimate(Long id) {
        Shipment s = get(id);
        return ahamove.estimate(s.getAddress().getAddress(), s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), cod(s));
    }

    /** Đặt xe Ahamove: the order should be confirmed (being made or ready). */
    @Transactional
    public ShipmentResponse book(Long id) {
        Shipment s = get(id);
        if (s.getStatus() != ShipmentStatus.PENDING) throw new BadRequestException("Đơn giao đã đặt xe hoặc đã kết thúc");
        OrderStatus os = s.getOrder().getStatus();
        if (os != OrderStatus.PREPARING && os != OrderStatus.READY && os != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ đặt xe cho đơn đã xác nhận");
        }
        AhamoveGateway.Booking b;
        try {
            b = ahamove.create(s.getOrder().getOrderCode(), s.getAddress().getAddress(), s.getAddress().getRecipientName(),
                    s.getAddress().getRecipientPhone(), cod(s), s.getOrder().getNote());
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        s.setPartner(AHAMOVE);
        s.setCarrier("Ahamove");
        s.setPartnerOrderId(b.orderId());
        s.setTrackingCode(b.orderId());
        s.setTrackingUrl(b.trackingUrl());
        s.setPartnerFee(b.fee());
        s.setDistanceKm(b.distanceKm());
        s.setDriverName(null);
        s.setDriverPhone(null);
        move(s, ShipmentStatus.BOOKED, "STAFF", b.status(), "Đặt xe Ahamove " + b.orderId(), null);
        return ShipmentResponse.from(s);
    }

    /** Hủy chuyến Ahamove (before pick-up): back to "chờ đặt xe". */
    @Transactional
    public ShipmentResponse cancelBooking(Long id, String reason) {
        Shipment s = get(id);
        if (!AHAMOVE.equals(s.getPartner()) || !isActive(s)) throw new BadRequestException("Không có chuyến Ahamove đang chờ để hủy");
        if (s.getStatus() == ShipmentStatus.DELIVERING) throw new BadRequestException("Tài xế đã lấy hàng, không hủy được chuyến");
        try {
            ahamove.cancel(s.getPartnerOrderId(), reason);
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        resetToPending(s, "STAFF", "Hủy chuyến Ahamove: " + reason, "CANCELLED");
        return ShipmentResponse.from(s);
    }

    /**
     * Ahamove webhook: {_id, status, path[{status}], supplier_name, supplier_id (phone)…}. The URL carries our token.
     * Returns false when the order is not ours.
     */
    @Transactional
    public boolean webhook(JsonNode body) {
        String orderId = body.path("_id").asText(body.path("order_id").asText(""));
        Shipment s = shipmentRepository.findByPartnerOrderId(orderId).orElse(null);
        if (s == null) return false;
        applyPartnerStatus(s, body.path("status").asText(""), lastPathStatus(body), body.path("supplier_name").asText(null),
                body.path("supplier_id").asText(null), body.toString());
        return true;
    }

    /** simulated Ahamove: drivers accept after a while and deliver after pick-up, through the webhook logic */
    @Scheduled(fixedDelayString = "${app.delivery.ahamove.simulate-ms:15000}", initialDelay = 20_000)
    @Transactional
    public void simulateTrips() {
        if (!ahamove.simulated()) return;
        var props = ahamove.props();
        LocalDateTime now = LocalDateTime.now();
        for (Shipment s : shipmentRepository.findByPartnerAndStatusIn(AHAMOVE,
                EnumSet.of(ShipmentStatus.BOOKED, ShipmentStatus.DRIVER_ACCEPTED, ShipmentStatus.DELIVERING))) {
            if (s.getStatus() == ShipmentStatus.BOOKED && s.getBookedAt() != null
                    && Duration.between(s.getBookedAt(), now).toSeconds() >= props.getSimulatedAcceptSeconds()) {
                applyPartnerStatus(s, "ACCEPTED", null, "Tài xế Ahamove (giả lập)", "0901234567", "{\"simulated\":true}");
            } else if (s.getStatus() == ShipmentStatus.DELIVERING) {
                // picked up when the shop handed the order over (Giao cho tài xế)
                LocalDateTime pickedUp = s.getOrder().getStatus() == OrderStatus.DELIVERING
                        ? (s.getUpdatedAt() != null ? s.getUpdatedAt() : lastChange(s)) : null;
                if (pickedUp != null && Duration.between(pickedUp, now).toSeconds() >= props.getSimulatedDeliverSeconds()) {
                    applyPartnerStatus(s, "COMPLETED", "COMPLETED", null, null, "{\"simulated\":true}");
                }
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------

    /** Ahamove status → ours; the order follows (picked up = đang giao, completed = hoàn tất). */
    void applyPartnerStatus(Shipment s, String status, String dropStatus, String driverName, String driverPhone, String raw) {
        if (s.getStatus() == ShipmentStatus.DELIVERED || s.getStatus() == ShipmentStatus.FAILED) return;
        if (driverName != null && !driverName.isBlank()) s.setDriverName(driverName);
        if (driverPhone != null && !driverPhone.isBlank()) s.setDriverPhone(driverPhone);
        switch (status) {
            case "IDLE", "ASSIGNING" -> move(s, ShipmentStatus.BOOKED, "PARTNER", status, null, raw);
            case "ACCEPTED", "CONFIRMING", "PAYING" -> move(s, ShipmentStatus.DRIVER_ACCEPTED, "PARTNER", status,
                    s.getDriverName() == null ? null : "Tài xế " + s.getDriverName(), raw);
            case "IN PROCESS" -> {
                if (s.getOrder().getStatus() == OrderStatus.READY) orderService.dispatch(s.getOrder().getId()); // tài xế đã lấy hàng
                move(s, ShipmentStatus.DELIVERING, "PARTNER", status, "Tài xế đã lấy hàng", raw);
            }
            case "COMPLETED" -> {
                if ("FAILED".equals(dropStatus) || "RETURNED".equals(dropStatus)) {
                    move(s, ShipmentStatus.FAILED, "PARTNER", status + "/" + dropStatus, "Giao không thành công — hàng trả về", raw);
                } else {
                    s.setDeliveredAt(LocalDateTime.now());
                    move(s, ShipmentStatus.DELIVERED, "PARTNER", status, "Đã giao", raw);
                    orderService.completeDelivered(s.getOrder());
                }
            }
            case "CANCELLED" -> resetToPending(s, "PARTNER", "Ahamove hủy chuyến — cần đặt xe lại", raw);
            default -> log.info("Ahamove {}: status {} ignored", s.getPartnerOrderId(), status);
        }
    }

    private void resetToPending(Shipment s, String source, String note, String raw) {
        s.setPartnerStatus("CANCELLED");
        s.setBookedAt(null);
        s.setDriverAcceptedAt(null);
        move(s, ShipmentStatus.PENDING, source, "CANCELLED", note, raw);
        s.setPartnerOrderId(null);
        s.setTrackingUrl(null);
    }

    private void move(Shipment s, ShipmentStatus to, String source, String partnerStatus, String note, String raw) {
        LocalDateTime now = LocalDateTime.now();
        if (to == ShipmentStatus.BOOKED && s.getBookedAt() == null) s.setBookedAt(now);
        if (to == ShipmentStatus.DRIVER_ACCEPTED && s.getDriverAcceptedAt() == null) s.setDriverAcceptedAt(now);
        if (to == ShipmentStatus.DELIVERED && s.getDeliveredAt() == null) s.setDeliveredAt(now);
        boolean changed = s.getStatus() != to;
        s.setStatus(to);
        if (partnerStatus != null) s.setPartnerStatus(partnerStatus);
        if (changed || raw != null) {
            eventRepository.save(ShipmentEvent.builder().shipment(s).source(source).status(to).partnerStatus(partnerStatus)
                    .note(note).raw(raw).createdBy("PARTNER".equals(source) ? "ahamove" : CurrentUser.username())
                    .createdAt(now).build());
        }
        if (to == ShipmentStatus.DELIVERED && "STAFF".equals(source)) orderService.completeDelivered(s.getOrder());
        else notificationService.notifyOrderChanged(OrderResponse.from(s.getOrder()));
    }

    private LocalDateTime lastChange(Shipment s) {
        var events = eventRepository.findByShipmentIdOrderByCreatedAtAscIdAsc(s.getId());
        return events.isEmpty() ? null : events.get(events.size() - 1).getCreatedAt();
    }

    private static boolean isActive(Shipment s) {
        return s.getStatus() == ShipmentStatus.BOOKED || s.getStatus() == ShipmentStatus.DRIVER_ACCEPTED
                || s.getStatus() == ShipmentStatus.DELIVERING;
    }

    /** tiền tài xế thu hộ: what the customer still owes in cash */
    private static BigDecimal cod(Shipment s) {
        return s.getOrder().getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && p.getMethod() == PaymentMethod.CASH)
                .map(com.coffeeshop.entity.Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String lastPathStatus(JsonNode body) {
        JsonNode path = body.path("path");
        return path.isArray() && path.size() > 1 ? path.get(path.size() - 1).path("status").asText(null) : null;
    }

    private Shipment get(Long id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn giao: " + id));
    }
}
