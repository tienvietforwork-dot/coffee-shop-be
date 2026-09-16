package com.coffeeshop.controller;

import com.coffeeshop.dto.request.OrderRequest;
import com.coffeeshop.dto.request.OrderStatusUpdateRequest;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : null;
        return orderService.create(request, actor);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody OrderStatusUpdateRequest request,
                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : null;
        return orderService.updateStatus(id, request.getStatus(), actor);
    }
}
