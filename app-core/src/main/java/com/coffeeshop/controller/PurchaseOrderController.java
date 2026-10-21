package com.coffeeshop.controller;

import com.coffeeshop.dto.request.GoodsReceiptRequest;
import com.coffeeshop.dto.request.PurchaseOrderRequest;
import com.coffeeshop.dto.request.ReasonRequest;
import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.dto.response.PurchaseOrderResponse;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phiếu đặt hàng nhà cung cấp; goods arriving against one are received here (phiếu nhập). */
@RestController
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    @GetMapping("/api/purchase-orders")
    @PreAuthorize(Perm.CAN_READ_PURCHASE_ORDERS)
    public List<PurchaseOrderResponse> search(@RequestParam(required = false) PurchaseOrderStatus status,
                                              @RequestParam(required = false) Long supplierId) {
        return orderService.search(status, supplierId);
    }

    @GetMapping("/api/purchase-orders/{id}")
    @PreAuthorize(Perm.CAN_READ_PURCHASE_ORDERS)
    public PurchaseOrderResponse find(@PathVariable Long id) {
        return orderService.find(id);
    }

    @PostMapping("/api/purchase-orders")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest request) {
        return orderService.create(request);
    }

    @PutMapping("/api/purchase-orders/{id}")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    public PurchaseOrderResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/api/purchase-orders/{id}")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }

    @PostMapping("/api/purchase-orders/{id}/send")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    public PurchaseOrderResponse send(@PathVariable Long id) {
        return orderService.send(id);
    }

    @PostMapping("/api/purchase-orders/{id}/cancel")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    public PurchaseOrderResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return orderService.cancel(id, request.reason());
    }

    @PostMapping("/api/purchase-orders/{id}/close")
    @PreAuthorize(Perm.CAN_PURCHASE_ORDERS)
    public PurchaseOrderResponse close(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return orderService.close(id, request.reason());
    }

    @PostMapping("/api/purchase-orders/{id}/receipts")
    @PreAuthorize(Perm.CAN_RECEIVE_GOODS)
    @ResponseStatus(HttpStatus.CREATED)
    public GoodsReceiptResponse receive(@PathVariable Long id, @Valid @RequestBody GoodsReceiptRequest request) {
        return orderService.receive(id, request);
    }

}
