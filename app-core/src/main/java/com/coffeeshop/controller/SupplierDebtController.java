package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ReasonRequest;
import com.coffeeshop.dto.request.SupplierPaymentRequest;
import com.coffeeshop.dto.response.SupplierDebtResponse;
import com.coffeeshop.dto.response.SupplierLedgerResponse;
import com.coffeeshop.dto.response.SupplierPaymentResponse;
import com.coffeeshop.export.ExcelSheet;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.SupplierDebtService;
import com.coffeeshop.service.SupplierPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Công nợ phải trả nhà cung cấp: tổng hợp, sổ chi tiết (+ Excel), phiếu chi. */
@RestController
@RequiredArgsConstructor
public class SupplierDebtController {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SupplierDebtService debtService;
    private final SupplierPaymentService paymentService;

    @GetMapping("/api/supplier-debts")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    public List<SupplierDebtResponse> summary() {
        return debtService.summary();
    }

    @GetMapping("/api/supplier-debts/{supplierId}/ledger")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    public SupplierLedgerResponse ledger(@PathVariable Long supplierId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return debtService.ledger(supplierId, from, to);
    }

    /** Sổ chi tiết công nợ as .xlsx. */
    @GetMapping("/api/supplier-debts/{supplierId}/ledger.xlsx")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    public ResponseEntity<byte[]> ledgerExcel(@PathVariable Long supplierId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SupplierLedgerResponse l = debtService.ledger(supplierId, from, to);
        ExcelSheet x = new ExcelSheet("Cong no")
                .title("SỔ CHI TIẾT CÔNG NỢ PHẢI TRẢ NHÀ CUNG CẤP")
                .line("Nhà cung cấp: " + l.supplierName() + (l.supplierTaxCode() == null ? "" : " · MST: " + l.supplierTaxCode()))
                .line("Kỳ: " + (from == null ? "đầu" : D.format(from)) + " – " + (to == null ? "nay" : D.format(to)))
                .header("Ngày", "Số chứng từ", "Diễn giải", "Hạn thanh toán", "Phát sinh tăng", "Phát sinh giảm", "Số dư")
                .total("", "", "Số dư đầu kỳ", "", null, null, l.openingBalance());
        for (var r : l.rows()) x.row(r.date(), r.code(), r.description(), r.dueDate(), nz(r.increase()), nz(r.decrease()), r.balance());
        x.total("", "", "Cộng phát sinh / số dư cuối kỳ", "", l.increase(), l.decrease(), l.closingBalance()).mergeTitle();
        return x.download("so-cong-no-" + slug(l.supplierName()));
    }

    @GetMapping("/api/supplier-payments")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    public List<SupplierPaymentResponse> payments(@RequestParam(required = false) Long supplierId,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return paymentService.search(supplierId, from, to);
    }

    @PostMapping("/api/supplier-payments")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierPaymentResponse pay(@Valid @RequestBody SupplierPaymentRequest request) {
        return paymentService.create(request);
    }

    @PostMapping("/api/supplier-payments/{id}/cancel")
    @PreAuthorize(Perm.CAN_SUPPLIER_DEBTS)
    public SupplierPaymentResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return paymentService.cancel(id, request.reason());
    }

    private static Object nz(java.math.BigDecimal v) {
        return v == null || v.signum() == 0 ? null : v;
    }

    static String slug(String s) {
        String ascii = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
