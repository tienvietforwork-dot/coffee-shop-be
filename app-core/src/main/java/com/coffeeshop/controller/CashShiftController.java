package com.coffeeshop.controller;

import com.coffeeshop.dto.request.CashShiftCloseRequest;
import com.coffeeshop.dto.response.CashShiftResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CashShiftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Ca thu ngân: ca đang mở, mở ca, thu / chi trong ca, chốt ca, lịch sử ca. */
@RestController
@RequestMapping("/api/cash-shifts")
@RequiredArgsConstructor
public class CashShiftController {

    private final CashShiftService shiftService;

    public record OpenRequest(@NotNull @DecimalMin("0") BigDecimal openingCash, @Size(max = 255) String note) {
    }

    public record MoveRequest(@NotBlank String type, @NotNull @DecimalMin("1") BigDecimal amount, @NotBlank @Size(max = 255) String reason) {
    }

    /** 204 when no shift is open; {@code suggestedOpening} header-free: see /suggested-opening */
    @GetMapping("/current")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public ResponseEntity<CashShiftResponse> current() {
        CashShiftResponse s = shiftService.current();
        return s == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(s);
    }

    @GetMapping("/suggested-opening")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public Map<String, BigDecimal> suggestedOpening() {
        return Map.of("openingCash", shiftService.suggestedOpening());
    }

    @GetMapping
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public List<CashShiftResponse> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return shiftService.search(from, to);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public CashShiftResponse find(@PathVariable Long id) {
        return shiftService.find(id);
    }

    @PostMapping("/open")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    @ResponseStatus(HttpStatus.CREATED)
    public CashShiftResponse open(@Valid @RequestBody OpenRequest request) {
        return shiftService.open(request.openingCash(), request.note());
    }

    @PostMapping("/current/movements")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public CashShiftResponse move(@Valid @RequestBody MoveRequest request) {
        return shiftService.move(request.type(), request.amount(), request.reason());
    }

    @PostMapping("/current/close")
    @PreAuthorize(Perm.CAN_CASH_SHIFTS)
    public CashShiftResponse close(@Valid @RequestBody CashShiftCloseRequest request) {
        return shiftService.close(request);
    }
}
