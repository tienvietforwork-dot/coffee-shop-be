package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Công nợ phải trả of one supplier. {@code balance} = đã nhập − trả hàng (trừ nợ) − đã chi; negative when the shop
 * paid in advance. {@code overdueAmount}: part of the open receipts past their due date.
 */
public record SupplierDebtResponse(Long supplierId, String supplierName, String phone, int paymentTermDays,
                                   BigDecimal purchasedAmount, BigDecimal returnedAmount, BigDecimal paidAmount,
                                   BigDecimal balance, BigDecimal overdueAmount, int openReceipts,
                                   LocalDate nextDueDate) {
}
