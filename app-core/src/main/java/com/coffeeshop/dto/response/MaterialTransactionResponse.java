package com.coffeeshop.dto.response;

import com.coffeeshop.entity.MaterialTransaction;
import com.coffeeshop.entity.enums.MaterialTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialTransactionResponse(Long id, Long batchId, Long materialId, String materialName, String unit,
                                          String staffName, Long orderItemId, MaterialTransactionType type,
                                          BigDecimal quantity, String note, LocalDateTime createdAt) {
    public static MaterialTransactionResponse from(MaterialTransaction t) {
        return new MaterialTransactionResponse(t.getId(), t.getBatch().getId(), t.getBatch().getMaterial().getId(),
                t.getBatch().getMaterial().getName(), t.getBatch().getMaterial().getUnit(),
                t.getStaff() == null ? null : t.getStaff().getFullName(),
                t.getOrderItem() == null ? null : t.getOrderItem().getId(),
                t.getType(), t.getQuantity(), t.getNote(), t.getCreatedAt());
    }
}
