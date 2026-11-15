package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.service.VoucherDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoucherDetailController {

    private final VoucherDetailService voucherDetailService;

    @PostMapping("/vouchers/voucher-detail/{id}/revoke")
    public ResponseEntity<?> revokeVoucherDetail(@PathVariable("id") Long id) {
        voucherDetailService.revokeVoucher(id);
        return ResponseEntity.ok("Thu hồi Voucher thành công!");
    }
}
