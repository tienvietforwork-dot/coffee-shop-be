package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sổ chi tiết công nợ phải trả nhà cung cấp for a period (also the data of the biên bản đối chiếu công nợ):
 * số dư đầu kỳ, phát sinh tăng (nhập hàng), phát sinh giảm (trả hàng, chi tiền), số dư cuối kỳ.
 */
public record SupplierLedgerResponse(Long supplierId, String supplierName, String supplierTaxCode,
                                     String supplierAddress, LocalDate from, LocalDate to,
                                     BigDecimal openingBalance, BigDecimal increase, BigDecimal decrease,
                                     BigDecimal closingBalance, List<Row> rows) {

    /** {@code type}: RECEIPT (phiếu nhập), RETURN (phiếu trả hàng, trừ nợ), PAYMENT (phiếu chi) */
    public record Row(LocalDateTime date, String type, Long documentId, String code, String description,
                      BigDecimal increase, BigDecimal decrease, BigDecimal balance, LocalDate dueDate) {
    }
}
