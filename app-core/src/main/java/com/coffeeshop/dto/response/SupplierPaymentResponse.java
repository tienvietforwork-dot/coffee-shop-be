package com.coffeeshop.dto.response;

import com.coffeeshop.entity.SupplierPayment;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Phiếu chi (mẫu 02-TT). */
public record SupplierPaymentResponse(Long id, String code, SupplierPaymentStatus status, Long supplierId,
                                      String supplierName, String supplierAddress, Long goodsReceiptId,
                                      String goodsReceiptCode, Long purchaseOrderId, String purchaseOrderCode,
                                      BigDecimal amount, PaymentMethod method, String reference, String payeeName,
                                      LocalDateTime paidAt, String staffName, Long documentImageId, String note,
                                      String cancelReason, LocalDateTime createdAt, String createdBy) {

    public static SupplierPaymentResponse from(SupplierPayment p) {
        var r = p.getGoodsReceipt();
        var o = p.getPurchaseOrder();
        return new SupplierPaymentResponse(p.getId(), p.getCode(), p.getStatus(), p.getSupplier().getId(),
                p.getSupplier().getName(), p.getSupplier().getAddress(), r == null ? null : r.getId(),
                r == null ? null : r.getCode(), o == null ? null : o.getId(), o == null ? null : o.getCode(),
                p.getAmount(), p.getMethod(), p.getReference(), p.getPayeeName(), p.getPaidAt(),
                p.getStaff() == null ? null : p.getStaff().getFullName(), p.getDocumentImageId(), p.getNote(),
                p.getCancelReason(), p.getCreatedAt(), p.getCreatedBy());
    }
}
