package com.coffeeshop.dto.response;

import com.coffeeshop.entity.VoucherDetail;
import com.coffeeshop.entity.enums.VoucherDiscountType;
import com.coffeeshop.entity.enums.VoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(Long id, String code, String voucherName, BigDecimal discountValue, VoucherDiscountType discountType, BigDecimal minOrderValue,
                              VoucherStatus status, LocalDateTime issuedAt, LocalDateTime expiresAt, String orderCode) {
    public static VoucherResponse from(VoucherDetail d) {
        return new VoucherResponse(d.getId(), d.getCode(), d.getVoucher().getName(), d.getVoucher().getDiscountValue(), d.getVoucher().getDiscountType(),
                d.getVoucher().getMinOrderValue(), d.getStatus(), d.getIssuedAt(), d.getExpiresAt(),
                d.getOrder() == null ? null : d.getOrder().getOrderCode());
    }
}
