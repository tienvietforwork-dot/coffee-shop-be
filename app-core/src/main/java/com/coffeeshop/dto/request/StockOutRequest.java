package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.TransactionReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Phiếu hủy / xuất khác: take {@code quantity} out of one lot, for a reason. */
public record StockOutRequest(
        @NotNull Long batchId,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull TransactionReason reason,
        @Size(max = 255) String note) {
}
