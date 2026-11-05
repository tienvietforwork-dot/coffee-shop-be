package com.coffeeshop.controller;

import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.DeliveryFeeService;
import com.coffeeshop.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** Giao hàng (quán tự giao): cửa hàng & giờ mở cửa, option giá theo km, khung giờ; gợi ý địa chỉ; lịch sử đơn giao. */
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryFeeService feeService;
    private final ShipmentService shipmentService;

    /** what the checkout needs: giờ mở cửa, miễn phí từ, đơn tối thiểu */
    public record PublicConfig(LocalTime openTime, LocalTime closeTime, BigDecimal freeShipThreshold, BigDecimal minOrderAmount) {
    }

    @GetMapping("/api/public/delivery-settings")
    public PublicConfig publicConfig() {
        var s = feeService.settings();
        return new PublicConfig(s.openTime(), s.closeTime(), s.freeShipThreshold(), s.minOrderAmount());
    }

    /** gợi ý địa chỉ khi gõ (ô địa chỉ ở trang đặt hàng, quầy, cấu hình) */
    @GetMapping("/api/public/address-suggest")
    public List<String> suggest(@RequestParam String q, @RequestParam(required = false) String ward,
                                @RequestParam(required = false) String province) {
        return feeService.suggest(q, ward, province);
    }

    @GetMapping("/api/delivery-settings")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryFeeService.Settings settings() {
        return feeService.settings();
    }

    @PutMapping("/api/delivery-settings")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryFeeService.Settings saveSettings(@RequestBody DeliveryFeeService.Settings request) {
        return feeService.saveSettings(request);
    }

    // ---- option giá ----

    @GetMapping("/api/delivery-fee-plans")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public List<DeliveryFeeService.Plan> plans() {
        return feeService.plans();
    }

    @PostMapping("/api/delivery-fee-plans")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryFeeService.Plan createPlan(@RequestBody DeliveryFeeService.Plan request) {
        return feeService.savePlan(null, request);
    }

    @PutMapping("/api/delivery-fee-plans/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryFeeService.Plan updatePlan(@PathVariable Long id, @RequestBody DeliveryFeeService.Plan request) {
        return feeService.savePlan(id, request);
    }

    @DeleteMapping("/api/delivery-fee-plans/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable Long id) {
        feeService.deletePlan(id);
    }

    // ---- khung giờ ----

    @GetMapping("/api/delivery-time-slots")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public List<DeliveryFeeService.Slot> slots() {
        return feeService.slots();
    }

    @PostMapping("/api/delivery-time-slots")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryFeeService.Slot createSlot(@RequestBody DeliveryFeeService.Slot request) {
        return feeService.saveSlot(null, request);
    }

    @PutMapping("/api/delivery-time-slots/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryFeeService.Slot updateSlot(@PathVariable Long id, @RequestBody DeliveryFeeService.Slot request) {
        return feeService.saveSlot(id, request);
    }

    @DeleteMapping("/api/delivery-time-slots/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(@PathVariable Long id) {
        feeService.deleteSlot(id);
    }

    @GetMapping("/api/shipments/{id}/events")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentResponse.Event> events(@PathVariable Long id) {
        return shipmentService.events(id);
    }
}
