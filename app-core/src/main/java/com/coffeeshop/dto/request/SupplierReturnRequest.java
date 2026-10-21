package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierReturnSettlement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Phiếu trả hàng nhập for lines of one phiếu nhập. {@code count}: in the receipt line's packaging (or the material's
 * unit when it was received loose). {@code unitPrice}: giá trả per unit of the material, defaults to the import price.
 * {@code complete}: save and send the goods back at once (otherwise the phiếu stays tạm).
 */
public record SupplierReturnRequest(
        @NotNull Long goodsReceiptId,
        @NotBlank @Size(max = 255) String reason,
        @NotNull SupplierReturnSettlement settlement,
        PaymentMethod refundMethod,
        @Size(max = 30) String adjustmentInvoiceNo,
        @Size(max = 255) String note,
        boolean complete,
        @NotEmpty List<@Valid Line> items) {

    public record Line(@NotNull Long receiptItemId, @NotNull @DecimalMin("0.01") BigDecimal count,
                       @DecimalMin("0") BigDecimal unitPrice, @Size(max = 255) String note) {
    }
}
