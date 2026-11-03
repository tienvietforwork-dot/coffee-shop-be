package com.coffeeshop.controller;

import com.coffeeshop.dto.response.InvoiceResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Hóa đơn điện tử: tra cứu, thông tin người mua (khách tự điền), thay thế, gửi lại phát hành lỗi. */
@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    public record ReplaceRequest(InvoiceService.Buyer buyer, String reason) {
    }

    // ---- khách ------------------------------------------------------------------

    @GetMapping("/api/public/orders/{orderCode}/invoices")
    public List<InvoiceResponse> forOrder(@PathVariable String orderCode) {
        return service.forOrderCode(orderCode);
    }

    /** "Xuất hóa đơn công ty": the customer types the buyer details (QR on the bill → this form). */
    @PostMapping("/api/public/orders/{orderCode}/invoice-request")
    public InvoiceResponse request(@PathVariable String orderCode, @RequestBody InvoiceService.Buyer buyer) {
        return service.setBuyer(orderCode, buyer);
    }

    // ---- quản lý ------------------------------------------------------------------

    @GetMapping("/api/invoices")
    @PreAuthorize(Perm.CAN_INVOICES)
    public List<InvoiceResponse> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword) {
        return service.search(from, to, status, keyword);
    }

    @GetMapping("/api/invoices/{id}")
    @PreAuthorize(Perm.CAN_INVOICES)
    public InvoiceResponse find(@PathVariable Long id) {
        return service.find(id);
    }

    @PostMapping("/api/invoices/order/{orderCode}/buyer")
    @PreAuthorize(Perm.CAN_INVOICES)
    public InvoiceResponse setBuyer(@PathVariable String orderCode, @RequestBody InvoiceService.Buyer buyer) {
        return service.setBuyer(orderCode, buyer);
    }

    @PostMapping("/api/invoices/{id}/replace")
    @PreAuthorize(Perm.CAN_INVOICES)
    public InvoiceResponse replace(@PathVariable Long id, @RequestBody ReplaceRequest request) {
        return service.replace(id, request.buyer(), request.reason());
    }

    @PostMapping("/api/invoices/{id}/retry")
    @PreAuthorize(Perm.CAN_INVOICES)
    public InvoiceResponse retry(@PathVariable Long id) {
        return service.retry(id);
    }

    /** Phát hành ngay những hóa đơn đang chờ (normally the job does it every few seconds). */
    @PostMapping("/api/invoices/issue-now")
    @PreAuthorize(Perm.CAN_INVOICES)
    public Map<String, Integer> issueNow() {
        return Map.of("issued", service.issueQueued());
    }
}
