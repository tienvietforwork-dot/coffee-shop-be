package com.coffeeshop.controller;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.request.ProduceRequest;
import com.coffeeshop.dto.request.StockAdjustRequest;
import com.coffeeshop.dto.request.StockOutRequest;
import com.coffeeshop.dto.request.StockImportRequest;
import com.coffeeshop.dto.response.BatchResponse;
import com.coffeeshop.dto.response.InventoryAlertsResponse;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.dto.response.MaterialTransactionResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.InventoryService;
import com.coffeeshop.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductionService productionService;

    @GetMapping("/api/materials")
    @PreAuthorize(Perm.CAN_READ_MATERIALS)
    public List<MaterialResponse> materials() {
        return inventoryService.findAll();
    }

    @PostMapping("/api/materials")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@Valid @RequestBody MaterialRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/api/materials/{id}")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return inventoryService.update(id, request);
    }

    @DeleteMapping("/api/materials/{id}")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inventoryService.delete(id);
    }

    @GetMapping("/api/materials/{id}/batches")
    @PreAuthorize(Perm.CAN_READ_MATERIALS)
    public List<BatchResponse> batches(@PathVariable Long id) {
        return inventoryService.batches(id);
    }

    @GetMapping("/api/inventory/transactions")
    @PreAuthorize(Perm.CAN_STOCK_HISTORY)
    public List<MaterialTransactionResponse> transactions(@RequestParam(required = false) Long materialId) {
        return inventoryService.transactions(materialId);
    }

    @GetMapping("/api/inventory/alerts")
    @PreAuthorize(Perm.CAN_INVENTORY_ALERTS)
    public InventoryAlertsResponse alerts() {
        return inventoryService.alerts();
    }

    @PostMapping("/api/inventory/import")
    @PreAuthorize(Perm.CAN_STOCK)
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse importStock(@Valid @RequestBody StockImportRequest request) {
        return inventoryService.importStock(request);
    }

    /** Phiếu hủy: one lot, with a disposal reason. */
    @PostMapping("/api/inventory/dispose")
    @PreAuthorize(Perm.CAN_STOCK)
    public MaterialTransactionResponse dispose(@Valid @RequestBody StockOutRequest request) {
        return inventoryService.dispose(request);
    }

    /** Xuất khác: one lot, returned to the supplier or used in-house. */
    @PostMapping("/api/inventory/export")
    @PreAuthorize(Perm.CAN_STOCK)
    public MaterialTransactionResponse exportOther(@Valid @RequestBody StockOutRequest request) {
        return inventoryService.exportOther(request);
    }

    @PostMapping("/api/inventory/adjust")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    public MaterialTransactionResponse adjust(@Valid @RequestBody StockAdjustRequest request) {
        return inventoryService.adjust(request);
    }

    /** Record made batches of a prepared material (e.g. cold brew): inputs leave stock, the batch is in stock at once. */
    @PostMapping("/api/inventory/produce")
    @PreAuthorize(Perm.CAN_STOCK)
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse produce(@Valid @RequestBody ProduceRequest request) {
        return productionService.produce(request);
    }
}
