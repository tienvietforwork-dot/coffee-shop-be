package com.coffeeshop.controller;

import com.coffeeshop.dto.request.IncidentResolveRequest;
import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.CustomerLookupResponse;
import com.coffeeshop.dto.response.DashboardResponse;
import com.coffeeshop.dto.response.IncidentResponse;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.dto.response.VoucherOptionResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CustomerService;
import com.coffeeshop.service.DashboardService;
import com.coffeeshop.service.DeliveryScheduleService;
import com.coffeeshop.service.IncidentService;
import com.coffeeshop.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Shipments, incidents, counter customer lookup and the dashboard. */
@RestController
@RequiredArgsConstructor
public class OperationsController {

    private final ShipmentService shipmentService;
    private final IncidentService incidentService;
    private final CustomerService customerService;
    private final DashboardService dashboardService;
    private final DeliveryScheduleService deliveryScheduleService;

    @GetMapping("/api/shipments")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentResponse> shipments() {
        return shipmentService.findAll();
    }

    /** đổi trạng thái giao; người giao chỉ đổi được đơn của mình */
    @PatchMapping("/api/shipments/{id}")
    @PreAuthorize(Perm.CAN_DELIVER)
    public ShipmentResponse updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest request) {
        return shipmentService.update(id, request);
    }

    public record AssignRequest(Long staffId) {
    }

    /** phân công / đổi người giao (staffId null = bỏ phân công) */
    @PutMapping("/api/shipments/{id}/shipper")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public ShipmentResponse assign(@PathVariable Long id, @RequestBody AssignRequest request) {
        return shipmentService.assign(id, request.staffId());
    }

    /** nhân viên đang làm + số đơn đang cầm, hôm nay đã giao / thất bại */
    @GetMapping("/api/shippers")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentService.Shipper> shippers() {
        return shipmentService.shippers();
    }

    /** đơn giao của tôi (người giao): chưa xong + đã xong hôm nay */
    @GetMapping("/api/my-deliveries")
    @PreAuthorize(Perm.CAN_MY_DELIVERIES)
    public List<ShipmentResponse> myDeliveries() {
        return shipmentService.mine();
    }

    /** lịch hẹn giao / nhận trong ngày (mặc định hôm nay), đơn phải pha sớm nhất lên đầu */
    @GetMapping("/api/delivery-schedule")
    @PreAuthorize(Perm.CAN_DELIVERY_SCHEDULE)
    public List<DeliveryScheduleService.Row> schedule(@RequestParam(required = false)
                                                      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                                      java.time.LocalDate date) {
        return deliveryScheduleService.schedule(date);
    }

    public record RouteRequest(List<Long> shipmentIds) {
    }

    /** tuyến giao gợi ý cho các đơn: thứ tự đi, đường vẽ trên bản đồ, link Google Maps */
    @PostMapping("/api/shipments/route")
    @PreAuthorize(Perm.CAN_DELIVER)
    public ShipmentService.RoutePlan route(@RequestBody RouteRequest request) {
        return shipmentService.route(request.shipmentIds());
    }

    @GetMapping("/api/incidents")
    @PreAuthorize(Perm.CAN_INCIDENTS)
    public List<IncidentResponse> incidents() {
        return incidentService.findAll();
    }

    /** Loại voucher đang phát được, kèm số lượt còn lại, để chọn khi đền bù sự cố. */
    @GetMapping("/api/incidents/voucher-options")
    @PreAuthorize(Perm.CAN_RESOLVE_INCIDENTS)
    public List<VoucherOptionResponse> voucherOptions() {
        return incidentService.voucherOptions();
    }

    @PostMapping("/api/incidents/{id}/resolve")
    @PreAuthorize(Perm.CAN_RESOLVE_INCIDENTS)
    public IncidentResponse resolve(@PathVariable Long id, @Valid @RequestBody IncidentResolveRequest request) {
        return incidentService.resolve(id, request);
    }

    @GetMapping("/api/customers/lookup")
    @PreAuthorize(Perm.CAN_LOOKUP_CUSTOMER)
    public CustomerLookupResponse lookupCustomer(@RequestParam String phone) {
        return customerService.lookup(phone);
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize(Perm.CAN_DASHBOARD)
    public DashboardResponse dashboard() {
        return dashboardService.summary();
    }
}
