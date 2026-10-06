package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, Long orderId, PaymentMethod method, BigDecimal amount, PaymentStatus status,
                              String transactionCode, LocalDateTime paidAt, BigDecimal refundAmount) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrder().getId(), p.getMethod(), p.getAmount(), p.getStatus(),
                p.getTransactionCode(), p.getPaidAt(), p.getRefundAmount());
    }
}
