package com.coffeeshop.entity.enums;

/**
 * Quán tự giao: PENDING (chờ giao) → DELIVERING (đang giao) → DELIVERED (đã giao) / FAILED (giao không thành công).
 * BOOKED, DRIVER_ACCEPTED: legacy (đặt xe ngoài), kept only so old rows still load.
 */
public enum ShipmentStatus {
    PENDING, @Deprecated BOOKED, @Deprecated DRIVER_ACCEPTED, DELIVERING, DELIVERED, FAILED
}
