package com.coffeeshop.controller;

import com.coffeeshop.dto.request.IncidentResolveRequest;
import com.coffeeshop.dto.request.ShipmentUpdateRequest;
import com.coffeeshop.dto.response.CustomerLookupResponse;
import com.coffeeshop.dto.response.DashboardResponse;
import com.coffeeshop.dto.response.IncidentResponse;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CustomerService;
import com.coffeeshop.service.DashboardService;
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

    @GetMapping("/api/shipments")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public List<ShipmentResponse> shipments() {
        return shipmentService.findAll();
    }

    @PatchMapping("/api/shipments/{id}")
    @PreAuthorize(Perm.CAN_SHIPMENTS)
    public ShipmentResponse updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest request) {
        return shipmentService.update(id, request);
    }

    @GetMapping("/api/incidents")
    @PreAuthorize(Perm.CAN_INCIDENTS)
    public List<IncidentResponse> incidents() {
        return incidentService.findAll();
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
