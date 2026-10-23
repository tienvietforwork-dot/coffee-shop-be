package com.coffeeshop.dto.response;

import java.math.BigDecimal;

/** Loại voucher để chọn khi phát: {@code remaining} = quantity − (đang giữ chỗ + đã dùng). */
public record VoucherOptionResponse(Long id, String name, BigDecimal discountValue, BigDecimal minOrderValue,
                                    int quantity, Integer validDays, long remaining) {
}
