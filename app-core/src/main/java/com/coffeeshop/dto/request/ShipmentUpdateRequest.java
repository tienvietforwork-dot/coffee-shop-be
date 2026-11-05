package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

/** DELIVERING (bắt đầu giao), DELIVERED (đã giao) hoặc FAILED (giao không thành công — cần ghi chú lý do) */
public record ShipmentUpdateRequest(@NotNull ShipmentStatus status, String note) {
}
