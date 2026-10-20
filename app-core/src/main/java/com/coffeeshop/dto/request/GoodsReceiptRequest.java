package com.coffeeshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Phiếu nhập theo phiếu đặt: what actually arrived for some of the order's lines. Each line becomes a lot. */
public record GoodsReceiptRequest(
        @Size(max = 255) String note,
        @NotEmpty List<@Valid Line> lines) {

    /**
     * {@code packageCount} (in the line's ordered packaging) or a loose {@code quantity} in the material's unit;
     * {@code unitPrice} per unit of the material, defaults to the ordered price.
     */
    public record Line(
            @NotNull Long itemId,
            @DecimalMin("0.01") BigDecimal packageCount,
            @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0") BigDecimal unitPrice,
            LocalDate expiryDate) {
    }
}
