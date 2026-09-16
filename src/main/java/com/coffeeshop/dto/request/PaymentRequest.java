package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private PaymentMethod method;

    @NotNull
    @Positive
    private BigDecimal amount;
}
