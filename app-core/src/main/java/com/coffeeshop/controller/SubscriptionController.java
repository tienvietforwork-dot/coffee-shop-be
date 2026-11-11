package com.coffeeshop.controller;

import com.coffeeshop.entity.enums.SubscriptionStatus;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gói giao cà phê định kỳ: nhân viên quản lý mọi gói; khách đã đăng nhập quản lý gói của mình. */
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    public record StatusRequest(SubscriptionStatus status) {
    }

    // ---- quản lý ----

    @GetMapping("/api/subscriptions")
    @PreAuthorize(Perm.CAN_SUBSCRIPTIONS)
    public List<SubscriptionService.View> all() {
        return service.findAll();
    }

    @PostMapping("/api/subscriptions")
    @PreAuthorize(Perm.CAN_SUBSCRIPTIONS)
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionService.View create(@Valid @RequestBody SubscriptionService.Request request) {
        return service.create(request, null);
    }

    @PutMapping("/api/subscriptions/{id}")
    @PreAuthorize(Perm.CAN_SUBSCRIPTIONS)
    public SubscriptionService.View update(@PathVariable Long id, @Valid @RequestBody SubscriptionService.Request request) {
        return service.update(id, request, null);
    }

    @PatchMapping("/api/subscriptions/{id}/status")
    @PreAuthorize(Perm.CAN_SUBSCRIPTIONS)
    public SubscriptionService.View status(@PathVariable Long id, @RequestBody StatusRequest request) {
        return service.setStatus(id, request.status(), null);
    }

    // ---- khách hàng ----

    @GetMapping("/api/account/subscriptions")
    @PreAuthorize(Perm.CAN_ACCOUNT)
    public List<SubscriptionService.View> mine() {
        return service.ofCustomer(CurrentUser.customerId());
    }

    @PostMapping("/api/account/subscriptions")
    @PreAuthorize(Perm.CAN_ACCOUNT)
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionService.View createMine(@Valid @RequestBody SubscriptionService.Request request) {
        return service.create(request, CurrentUser.customerId());
    }

    @PutMapping("/api/account/subscriptions/{id}")
    @PreAuthorize(Perm.CAN_ACCOUNT)
    public SubscriptionService.View updateMine(@PathVariable Long id, @Valid @RequestBody SubscriptionService.Request request) {
        return service.update(id, request, CurrentUser.customerId());
    }

    @PatchMapping("/api/account/subscriptions/{id}/status")
    @PreAuthorize(Perm.CAN_ACCOUNT)
    public SubscriptionService.View statusMine(@PathVariable Long id, @RequestBody StatusRequest request) {
        return service.setStatus(id, request.status(), CurrentUser.customerId());
    }
}
