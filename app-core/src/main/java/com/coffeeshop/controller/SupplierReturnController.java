package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ReasonRequest;
import com.coffeeshop.dto.request.SupplierReturnRequest;
import com.coffeeshop.dto.response.SupplierReturnResponse;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.SupplierReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phiếu trả hàng nhập. */
@RestController
@RequestMapping("/api/supplier-returns")
@RequiredArgsConstructor
public class SupplierReturnController {

    private final SupplierReturnService returnService;

    @GetMapping
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    public List<SupplierReturnResponse> search(@RequestParam(required = false) Long supplierId,
                                               @RequestParam(required = false) SupplierReturnStatus status,
                                               @RequestParam(required = false) Long goodsReceiptId) {
        return returnService.search(supplierId, status, goodsReceiptId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    public SupplierReturnResponse find(@PathVariable Long id) {
        return returnService.find(id);
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierReturnResponse create(@Valid @RequestBody SupplierReturnRequest request) {
        return returnService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    public SupplierReturnResponse update(@PathVariable Long id, @Valid @RequestBody SupplierReturnRequest request) {
        return returnService.update(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    public SupplierReturnResponse complete(@PathVariable Long id) {
        return returnService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Perm.CAN_SUPPLIER_RETURNS)
    public SupplierReturnResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return returnService.cancel(id, request.reason());
    }
}
