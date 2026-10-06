package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.VoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(Long id, String code, BigDecimal discountValue, BigDecimal minOrderValue,
                              VoucherStatus status, LocalDateTime issuedAt, LocalDateTime expiresAt, String orderCode) {
    public static VoucherResponse from(Voucher v) {
        return new VoucherResponse(v.getId(), v.getCode(), v.getDiscountValue(), v.getMinOrderValue(), v.getStatus(),
                v.getIssuedAt(), v.getExpiresAt(), v.getOrder() == null ? null : v.getOrder().getOrderCode());
    }
}
