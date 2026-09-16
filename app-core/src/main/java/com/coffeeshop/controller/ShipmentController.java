package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ShipmentAssignRequest;
import com.coffeeshop.dto.request.ShipmentCreateRequest;
import com.coffeeshop.dto.request.ShipmentStatusUpdateRequest;
import com.coffeeshop.dto.response.ShipmentResponse;
import com.coffeeshop.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public List<ShipmentResponse> findAll() {
        return shipmentService.findAll();
    }

    @GetMapping("/{id}")
    public ShipmentResponse findById(@PathVariable Long id) {
        return shipmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse create(@Valid @RequestBody ShipmentCreateRequest request) {
        return shipmentService.createForOrder(request.getOrderId(), request.getAddress());
    }

    @PatchMapping("/{id}/assign")
    public ShipmentResponse assign(@PathVariable Long id, @Valid @RequestBody ShipmentAssignRequest request) {
        return shipmentService.assignShipper(id, request);
    }

    @PatchMapping("/{id}/status")
    public ShipmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ShipmentStatusUpdateRequest request) {
        return shipmentService.updateStatus(id, request);
    }
}
