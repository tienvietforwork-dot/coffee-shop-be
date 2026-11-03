package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Money;
import com.coffeeshop.entity.SupplierReturn;
import com.coffeeshop.entity.SupplierReturnItem;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierReturnSettlement;
import com.coffeeshop.entity.enums.SupplierReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Phiếu trả hàng nhập. */
public record SupplierReturnResponse(Long id, String code, SupplierReturnStatus status, Long supplierId,
                                     String supplierName, String supplierTaxCode, String supplierAddress,
                                     Long goodsReceiptId, String goodsReceiptCode, String receiptInvoiceNo,
                                     String staffName, String reason, SupplierReturnSettlement settlement,
                                     PaymentMethod refundMethod, String adjustmentInvoiceNo, BigDecimal goodsAmount,
                                     BigDecimal vatRate, BigDecimal vatAmount, BigDecimal totalAmount,
                                     LocalDateTime completedAt, String cancelReason, String note,
                                     LocalDateTime createdAt, String createdBy, List<Line> items) {

    public record Line(Long id, Long receiptItemId, Long batchId, Long materialId, String materialName, String unit,
                       String packageName, BigDecimal packageSize, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal amount, String note) {
        static Line from(SupplierReturnItem i) {
            var ri = i.getGoodsReceiptItem();
            return new Line(i.getId(), ri.getId(), ri.getBatch() == null ? null : ri.getBatch().getId(),
                    ri.getMaterial().getId(), ri.getMaterial().getName(), ri.getMaterial().getUnit(),
                    ri.getPackageName(), ri.getPackageSize(), i.getQuantity(), i.getUnitPrice(), i.amount(), i.getNote());
        }
    }

    public static SupplierReturnResponse from(SupplierReturn r) {
        var s = r.getSupplier();
        var receipt = r.getGoodsReceipt();
        return new SupplierReturnResponse(r.getId(), r.getCode(), r.getStatus(), s.getId(), s.getName(), s.getTaxCode(),
                s.getAddress(), receipt.getId(), receipt.getCode(), receipt.getInvoiceNo(),
                r.getStaff() == null ? null : r.getStaff().getFullName(), r.getReason(), r.getSettlement(),
                r.getRefundMethod(), r.getAdjustmentInvoiceNo(), r.goodsAmount(), r.getVatRate(),
                Money.vat(r.goodsAmount(), BigDecimal.ZERO, r.getVatRate()), r.totalAmount(), r.getCompletedAt(),
                r.getCancelReason(), r.getNote(), r.getCreatedAt(), r.getCreatedBy(),
                r.getItems().stream().map(Line::from).toList());
    }
}
