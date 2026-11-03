package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.StockQuality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Phiếu kiểm kê (tạm). {@code materialIds}: lots of these materials still in stock are added with their book
 * quantity (empty: every material). {@code counts}: what was counted so far, by line id.
 */
public record StocktakeRequest(
        @Size(max = 255) String members,
        @Size(max = 255) String note,
        List<Long> materialIds,
        List<@Valid Count> counts) {

    public record Count(@NotNull Long itemId, @DecimalMin("0") BigDecimal countedQuantity, StockQuality quality,
                        @Size(max = 255) String reason) {
    }
}
