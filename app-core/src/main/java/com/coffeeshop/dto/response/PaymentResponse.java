package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code refundRef} / {@code refundNote} / {@code refundedAt}: hoàn tiền (qua VNPAY hoặc chuyển khoản tay) */
public record PaymentResponse(Long id, Long orderId, PaymentMethod method, BigDecimal amount, PaymentStatus status,
                              String transactionCode, LocalDateTime paidAt, BigDecimal refundAmount,
                              LocalDateTime refundedAt, String refundRef, String refundNote, String orderCode,
                              String customerName, String customerPhone, String cancelReason) {
    public static PaymentResponse from(Payment p) {
        var o = p.getOrder();
        return new PaymentResponse(p.getId(), o.getId(), p.getMethod(), p.getAmount(), p.getStatus(),
                p.getTransactionCode(), p.getPaidAt(), p.getRefundAmount(), p.getRefundedAt(), p.getRefundRef(),
                p.getRefundNote(), o.getOrderCode(), o.getCustomer() == null ? null : o.getCustomer().getFullName(),
                o.getCustomer() == null ? null : o.getCustomer().getPhone(), o.getCancelReason());
    }
}
