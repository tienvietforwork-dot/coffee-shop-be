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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** Personalized for signed-in customers; guests get privacy-safe popular products. */
    @GetMapping("/recommendations")
    public java.util.List<Long> recommendations() {
        Long customer = com.coffeeshop.security.CurrentUser.customerId();
        if(customer == null) return jdbc.query("""
            SELECT c.id FROM coffees c LEFT JOIN order_items oi ON oi.coffee_id=c.id AND oi.del_flag=false
            LEFT JOIN orders o ON o.id=oi.order_id AND o.status='COMPLETED' AND o.del_flag=false
            WHERE c.del_flag=false AND c.status='AVAILABLE' AND c.auto_sold_out=false
            GROUP BY c.id ORDER BY COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END),0) DESC,c.id LIMIT 8
        """,(r,n)->r.getLong(1));
        // Customer identity is read from verified JWT, not a client-supplied customer id.
        return jdbc.query("""
            WITH favorites AS (
              SELECT c.category_id, SUM(oi.quantity) AS weight
              FROM orders o JOIN order_items oi ON oi.order_id=o.id AND oi.del_flag=false
              JOIN coffees c ON c.id=oi.coffee_id
              WHERE o.customer_id=? AND o.status='COMPLETED' AND o.del_flag=false
              GROUP BY c.category_id
            ), trend AS (
              SELECT oi.coffee_id,SUM(oi.quantity) AS units FROM order_items oi
              JOIN orders o ON o.id=oi.order_id WHERE o.status='COMPLETED' AND o.del_flag=false AND oi.del_flag=false
              GROUP BY oi.coffee_id
            )
            SELECT c.id FROM coffees c
            LEFT JOIN favorites f ON f.category_id=c.category_id
            LEFT JOIN trend t ON t.coffee_id=c.id
            WHERE c.del_flag=false AND c.status='AVAILABLE' AND c.auto_sold_out=false
            ORDER BY COALESCE(f.weight,0)*1000+COALESCE(t.units,0) DESC,c.id LIMIT 8
        """,(r,n)->r.getLong(1),customer);
    }

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

    public record AddBundleRequest(Long bundleId, Integer quantity, java.util.List<Long> optionalItemIds) {}

    @PostMapping("/carts/{sessionCode}/bundles")
    public CartResponse addBundle(@PathVariable String sessionCode, @RequestBody AddBundleRequest req) {
        if(req.bundleId()==null || req.quantity()==null) throw new com.coffeeshop.exception.BadRequestException("Thiếu thông tin combo");
        return cartService.addBundle(sessionCode,req.bundleId(),req.quantity(),req.optionalItemIds());
    }
    @DeleteMapping("/carts/{sessionCode}/bundles/{lineId}")
    public CartResponse removeBundle(@PathVariable String sessionCode,@PathVariable Long lineId) {
        return cartService.removeBundle(sessionCode,lineId);
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
