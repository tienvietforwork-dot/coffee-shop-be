package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.MaterialTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialTransactionRequest {

    @NotNull
    private Long materialId;

    @NotNull
    private MaterialTransactionType type;

    @NotNull
    @Positive
    private BigDecimal quantity;

    private String reason;
}
