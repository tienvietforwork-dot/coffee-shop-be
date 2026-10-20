package com.coffeeshop.dto.response;

import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.MaterialBatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Phiếu nhập kho; {@code lines} are the lots it brought in. */
public record GoodsReceiptResponse(Long id, String code, Long supplierId, String supplierName, Long purchaseOrderId,
                                   String purchaseOrderCode, String staffName, String note, LocalDateTime createdAt,
                                   String createdBy, BigDecimal totalAmount, List<BatchResponse> lines) {

    public static GoodsReceiptResponse from(GoodsReceipt r) {
        return from(r, r.getBatches());
    }

    public static GoodsReceiptResponse from(GoodsReceipt r, List<MaterialBatch> batches) {
        var po = r.getPurchaseOrder();
        return new GoodsReceiptResponse(r.getId(), r.getCode(), r.getSupplier().getId(), r.getSupplier().getName(),
                po == null ? null : po.getId(), po == null ? null : po.getCode(),
                r.getStaff() == null ? null : r.getStaff().getFullName(), r.getNote(), r.getCreatedAt(), r.getCreatedBy(),
                batches.stream().map(b -> b.getUnitCost() == null ? BigDecimal.ZERO : b.getUnitCost().multiply(b.getImportQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                batches.stream().map(BatchResponse::from).toList());
    }
}
