package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ReasonRequest;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.entity.DeliveryZone;
import com.coffeeshop.integration.ahamove.AhamoveGateway;
import com.coffeeshop.integration.ahamove.AhamoveProperties;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.DeliveryZoneService;
import com.coffeeshop.service.ShipmentService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Khu vực & phí giao hàng; đặt xe Ahamove (báo giá, đặt, hủy, lịch sử chuyến) and its webhook. */
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryZoneService zoneService;
    private final ShipmentService shipmentService;
    private final AhamoveProperties ahamoveProps;

    /** what the checkout shows: quận giao được, phí, miễn phí từ, đơn tối thiểu */
    public record PublicZone(String name, List<String> districts, BigDecimal fee, BigDecimal freeShipThreshold,
                             BigDecimal minOrderAmount, Integer estimatedMinutes) {
        static PublicZone from(DeliveryZone z) {
            return new PublicZone(z.getName(), z.districtList(), z.getFee(), z.getFreeShipThreshold(), z.getMinOrderAmount(),
                    z.getEstimatedMinutes());
        }
    }

    @GetMapping("/api/public/delivery-zones")
    public List<PublicZone> publicZones() {
        return zoneService.active().stream().map(PublicZone::from).toList();
    }

    @GetMapping("/api/delivery-zones")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public List<DeliveryZone> zones() {
        return zoneService.findAll();
    }

    @PostMapping("/api/delivery-zones")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryZone create(@RequestBody DeliveryZoneService.ZoneRequest request) {
        return zoneService.save(null, request);
    }

    @PutMapping("/api/delivery-zones/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryZone update(@PathVariable Long id, @RequestBody DeliveryZoneService.ZoneRequest request) {
        return zoneService.save(id, request);
    }

    @DeleteMapping("/api/delivery-zones/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        zoneService.delete(id);
    }

    // ---- Ahamove -------------------------------------------------------------

    @GetMapping("/api/shipments/{id}/ahamove/estimate")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public AhamoveGateway.Estimate estimate(@PathVariable Long id) {
        return shipmentService.estimate(id);
    }

    @PostMapping("/api/shipments/{id}/ahamove/book")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public ShipmentResponse book(@PathVariable Long id) {
        return shipmentService.book(id);
    }

    @PostMapping("/api/shipments/{id}/ahamove/cancel")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public ShipmentResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return shipmentService.cancelBooking(id, request.reason());
    }

    @GetMapping("/api/shipments/{id}/events")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentResponse.Event> events(@PathVariable Long id) {
        return shipmentService.events(id);
    }

    /** Ahamove → us on every status change; the registered URL carries ?token=… */
    @PostMapping("/api/public/delivery/ahamove/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestParam(required = false) String token, @RequestBody JsonNode body) {
        if (token == null || !token.equals(ahamoveProps.getWebhookToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid token"));
        }
        boolean ours = shipmentService.webhook(body);
        return ResponseEntity.ok(Map.of("message", ours ? "ok" : "unknown order"));
    }
}
