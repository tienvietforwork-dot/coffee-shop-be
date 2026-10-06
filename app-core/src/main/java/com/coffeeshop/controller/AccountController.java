package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ProfileRequest;
import com.coffeeshop.dto.response.AccountResponse;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PointHistoryResponse;
import com.coffeeshop.dto.response.VoucherResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Khách hàng đã đăng nhập: hồ sơ, đơn hàng, voucher, điểm tích lũy của chính mình. */
@RestController
@RequestMapping("/api/account")
@PreAuthorize(Perm.CAN_ACCOUNT)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountResponse profile() {
        return accountService.profile();
    }

    @PutMapping
    public AccountResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        return accountService.updateProfile(request);
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders() {
        return accountService.orders();
    }

    @GetMapping("/vouchers")
    public List<VoucherResponse> vouchers() {
        return accountService.vouchers();
    }

    @GetMapping("/points")
    public List<PointHistoryResponse> points() {
        return accountService.points();
    }
}
