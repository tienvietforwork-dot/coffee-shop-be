package com.coffeeshop.entity.enums;

/** Giao dịch cổng thanh toán: chờ khách trả, thành công, thất bại (khách hủy, sai OTP…), hết hạn (không trả trong 15 phút). */
public enum PaymentTransactionStatus {
    PENDING, SUCCESS, FAILED, EXPIRED
}
