package com.coffeeshop.controller;

import com.coffeeshop.dto.request.GoodsReceiptRequest;
import com.coffeeshop.dto.request.ReasonRequest;
import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.GoodsReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Phiếu nhập kho: list / detail, nhập lẻ (không theo phiếu đặt), hủy phiếu. Receiving a purchase order is on its controller. */
@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService receiptService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_RECEIPTS)
    public List<GoodsReceiptResponse> search(@RequestParam(required = false) Long supplierId,
                                             @RequestParam(required = false) Long purchaseOrderId,
                                             @RequestParam(required = false) GoodsReceiptStatus status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return receiptService.search(supplierId, purchaseOrderId, status, from, to);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Perm.CAN_READ_RECEIPTS)
    public GoodsReceiptResponse find(@PathVariable Long id) {
        return receiptService.find(id);
    }

    /** Nhập lẻ: the supplier is required. */
    @PostMapping
    @PreAuthorize(Perm.CAN_STOCK)
    @ResponseStatus(HttpStatus.CREATED)
    public GoodsReceiptResponse createLoose(@Valid @RequestBody GoodsReceiptRequest request) {
        return receiptService.createLoose(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Perm.CAN_CANCEL_RECEIPT)
    public GoodsReceiptResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return receiptService.cancel(id, request.reason());
    }
}
