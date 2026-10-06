package com.coffeeshop.controller;

import com.coffeeshop.dto.request.*;
import com.coffeeshop.dto.response.*;
import com.coffeeshop.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Guest-facing API (no login): menu, QR table, cart, checkout, order tracking. */
@Tag(name = "Public (khách hàng)")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CoffeeService coffeeService;
    private final DiningTableService tableService;
    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping("/menu")
    public MenuResponse menu() {
        return coffeeService.menu();
    }

    @GetMapping("/tables/{qrCode}")
    public DiningTableResponse table(@PathVariable String qrCode) {
        return tableService.findByQr(qrCode);
    }

    @PostMapping("/carts")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart(@RequestBody(required = false) CreateCartRequest request) {
        return cartService.create(request);
    }

    @GetMapping("/carts/{sessionCode}")
    public CartResponse cart(@PathVariable String sessionCode) {
        return cartService.get(sessionCode);
    }

    @PostMapping("/carts/{sessionCode}/items")
    public CartResponse addItem(@PathVariable String sessionCode, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(sessionCode, request);
    }

    @PutMapping("/carts/{sessionCode}/items/{itemId}")
    public CartResponse updateItem(@PathVariable String sessionCode, @PathVariable Long itemId,
                                   @Valid @RequestBody CartItemUpdateRequest request) {
        return cartService.updateItem(sessionCode, itemId, request);
    }

    @DeleteMapping("/carts/{sessionCode}/items/{itemId}")
    public CartResponse removeItem(@PathVariable String sessionCode, @PathVariable Long itemId) {
        return cartService.removeItem(sessionCode, itemId);
    }

    @PostMapping("/carts/{sessionCode}/quote")
    public PriceQuoteResponse quote(@PathVariable String sessionCode, @Valid @RequestBody CartQuoteRequest request) {
        return orderService.previewCart(sessionCode, request.orderType(), request.customerPhone(), request.voucherCode());
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping("/orders/{orderCode}")
    public OrderResponse track(@PathVariable String orderCode) {
        return orderService.findByCode(orderCode);
    }

    @PostMapping("/orders/{orderCode}/cancel")
    public OrderResponse cancel(@PathVariable String orderCode, @Valid @RequestBody ReasonRequest request) {
        return orderService.cancelByCustomer(orderCode, request.reason());
    }
}
