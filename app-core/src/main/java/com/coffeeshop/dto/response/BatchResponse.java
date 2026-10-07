package com.coffeeshop.dto.response;

import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.MaterialTransaction;
import com.coffeeshop.entity.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** {@code inputs}: for a prepared-material batch, the raw batches it was made from. */
public record BatchResponse(Long id, Long materialId, String materialName, String unit, BigDecimal importQuantity,
                            BigDecimal remainingQuantity, BigDecimal unitCost, LocalDate expiryDate,
                            BatchStatus status, LocalDateTime createdAt, LocalDateTime readyAt, LocalDateTime expiresAt,
                            List<Input> inputs) {

    public record Input(Long batchId, String materialName, String unit, BigDecimal quantity) {
    }

    public static BatchResponse from(MaterialBatch b) {
        return from(b, null);
    }

    public static BatchResponse from(MaterialBatch b, List<MaterialTransaction> uses) {
        return new BatchResponse(b.getId(), b.getMaterial().getId(), b.getMaterial().getName(),
                b.getMaterial().getUnit(), b.getImportQuantity(), b.getRemainingQuantity(), b.getUnitCost(),
                b.getExpiryDate(), b.getStatus(), b.getCreatedAt(), b.getReadyAt(), b.getExpiresAt(),
                uses == null ? null : uses.stream().map(t -> new Input(t.getBatch().getId(),
                        t.getBatch().getMaterial().getName(), t.getBatch().getMaterial().getUnit(), t.getQuantity().negate())).toList());
    }
}
