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

/** Giao hàng (quán tự giao): cửa hàng & giờ mở cửa, khu vực giao + phí ship theo tỉnh / phường; gợi ý địa chỉ; lịch sử đơn giao. */
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryFeeService feeService;
    private final ShipmentService shipmentService;

    /** what the checkout needs: địa chỉ quán, giờ mở cửa, miễn phí từ, đơn tối thiểu, các tỉnh giao được */
    public record PublicConfig(String shopAddress, LocalTime openTime, LocalTime closeTime, BigDecimal freeShipThreshold,
                               BigDecimal minOrderAmount, BigDecimal speedKmh, List<String> provinces) {
    }

    @GetMapping("/api/public/delivery-settings")
    public PublicConfig publicConfig() {
        var s = feeService.settings();
        return new PublicConfig(s.shopAddress(), s.openTime(), s.closeTime(), s.freeShipThreshold(), s.minOrderAmount(), s.speedKmh(),
                feeService.areas().stream().filter(DeliveryFeeService.Area::active).map(DeliveryFeeService.Area::name).toList());
    }

    /** phường / xã đã chọn có giao được không, phí bao nhiêu (trước khi nhập số nhà) */
    @GetMapping("/api/public/delivery-area")
    public DeliveryFeeService.AreaCheck area(@RequestParam String ward, @RequestParam(required = false) String province) {
        return feeService.checkArea(ward, province);
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

    // ---- khu vực giao: tỉnh + phí, phường phí riêng / không giao ----

    @GetMapping("/api/delivery-areas")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public List<DeliveryFeeService.Area> areas() {
        return feeService.areas();
    }

    @PostMapping("/api/delivery-areas")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryFeeService.Area createArea(@RequestBody DeliveryFeeService.Area request) {
        return feeService.saveArea(null, request);
    }

    @PutMapping("/api/delivery-areas/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    public DeliveryFeeService.Area updateArea(@PathVariable Long id, @RequestBody DeliveryFeeService.Area request) {
        return feeService.saveArea(id, request);
    }

    @DeleteMapping("/api/delivery-areas/{id}")
    @PreAuthorize(Perm.CAN_DELIVERY_ZONES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArea(@PathVariable Long id) {
        feeService.deleteArea(id);
    }

    @GetMapping("/api/shipments/{id}/events")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentResponse.Event> events(@PathVariable Long id) {
        return shipmentService.events(id);
    }
}
