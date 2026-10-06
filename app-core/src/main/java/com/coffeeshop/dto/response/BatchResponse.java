package com.coffeeshop.dto.response;

import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchResponse(Long id, Long materialId, String materialName, String unit, BigDecimal importQuantity,
                            BigDecimal remainingQuantity, BigDecimal unitCost, LocalDate expiryDate,
                            BatchStatus status) {
    public static BatchResponse from(MaterialBatch b) {
        return new BatchResponse(b.getId(), b.getMaterial().getId(), b.getMaterial().getName(),
                b.getMaterial().getUnit(), b.getImportQuantity(), b.getRemainingQuantity(), b.getUnitCost(),
                b.getExpiryDate(), b.getStatus());
    }
}
