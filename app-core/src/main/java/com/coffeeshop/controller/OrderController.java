package com.coffeeshop.controller;

import com.coffeeshop.dto.request.*;
import com.coffeeshop.dto.response.IncidentResponse;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PaymentResponse;
import com.coffeeshop.dto.response.PriceQuoteResponse;
import com.coffeeshop.entity.enums.OrderChannel;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.IncidentService;
import com.coffeeshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Không có phân công: nhân viên nào thao tác thì được ghi nhận vào orders.staff_id. */
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final IncidentService incidentService;

    @GetMapping("/api/orders")
    @PreAuthorize(Perm.CAN_READ_ORDERS)
    public List<OrderResponse> search(@RequestParam(required = false) OrderStatus status,
                                      @RequestParam(required = false) OrderChannel channel,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return orderService.search(status, channel, from, to);
    }

    @GetMapping("/api/orders/{id}")
    @PreAuthorize(Perm.CAN_READ_ORDERS)
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping("/api/orders/quote")
    @PreAuthorize(Perm.CAN_POS)
    public PriceQuoteResponse quote(@Valid @RequestBody PricePreviewRequest request) {
        return orderService.preview(request);
    }

    /** Order tại quầy. */
    @PostMapping("/api/orders")
    @PreAuthorize(Perm.CAN_POS)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CounterOrderRequest request) {
        return orderService.createAtCounter(request);
    }

    @PostMapping("/api/orders/{id}/confirm")
    @PreAuthorize(Perm.CAN_ORDERS)
    public OrderResponse confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/api/orders/{id}/reject")
    @PreAuthorize(Perm.CAN_ORDERS)
    public OrderResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return orderService.reject(id, request.reason());
    }

    @PostMapping("/api/orders/{id}/cancel")
    @PreAuthorize(Perm.CAN_ORDERS)
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return orderService.cancel(id, request.reason());
    }

    @PostMapping("/api/orders/{id}/prepare")
    @PreAuthorize(Perm.CAN_ORDERS)
    public OrderResponse prepare(@PathVariable Long id) {
        return orderService.startPreparing(id);
    }

    @PostMapping("/api/orders/{id}/ready")
    @PreAuthorize(Perm.CAN_ORDERS)
    public OrderResponse ready(@PathVariable Long id) {
        return orderService.markReady(id);
    }

    @PostMapping("/api/orders/{id}/dispatch")
    @PreAuthorize("hasAnyAuthority('orders','shipments')")
    public OrderResponse dispatch(@PathVariable Long id) {
        return orderService.dispatch(id);
    }

    @PostMapping("/api/orders/{id}/complete")
    @PreAuthorize("hasAnyAuthority('orders','shipments')")
    public OrderResponse complete(@PathVariable Long id) {
        return orderService.complete(id);
    }

    @PostMapping("/api/orders/{id}/collect-payment")
    @PreAuthorize(Perm.CAN_COLLECT)
    public OrderResponse collectPayment(@PathVariable Long id) {
        return orderService.collectPayment(id);
    }

    @PostMapping("/api/payments/{paymentId}/refund")
    @PreAuthorize(Perm.CAN_REFUND)
    public PaymentResponse refund(@PathVariable Long paymentId, @Valid @RequestBody RefundRequest request) {
        return orderService.refund(paymentId, request.amount());
    }

    @GetMapping("/api/orders/{id}/incidents")
    @PreAuthorize(Perm.CAN_INCIDENTS)
    public List<IncidentResponse> incidents(@PathVariable Long id) {
        return incidentService.findByOrder(id);
    }

    @PostMapping("/api/orders/{id}/incidents")
    @PreAuthorize(Perm.CAN_INCIDENTS)
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse reportIncident(@PathVariable Long id, @Valid @RequestBody IncidentRequest request) {
        return incidentService.report(id, request);
    }
}
