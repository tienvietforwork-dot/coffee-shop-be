package com.coffeeshop.controller;

import com.coffeeshop.dto.request.SupplierRequest;
import com.coffeeshop.dto.response.SupplierResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_SUPPLIERS)
    public List<SupplierResponse> findAll() {
        return supplierService.findAll();
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_SUPPLIERS)
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_SUPPLIERS)
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    /** Ngừng hợp tác. */
    @DeleteMapping("/{id}")
    @PreAuthorize(Perm.CAN_SUPPLIERS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
