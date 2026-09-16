package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String method;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .method(p.getMethod().name())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .paidAt(p.getPaidAt())
                .build();
    }
}
