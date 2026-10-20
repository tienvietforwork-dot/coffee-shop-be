package com.coffeeshop.entity.enums;

/**
 * Phiếu đặt hàng nhà cung cấp: DRAFT (nháp: sửa / xóa / gửi) → ORDERED (đã đặt: nhận hàng, hủy khi chưa nhận gì)
 * → PARTIALLY_RECEIVED (nhận một phần: nhận tiếp hoặc đóng phiếu) → COMPLETED (nhận đủ hoặc đã đóng); CANCELLED.
 */
public enum PurchaseOrderStatus {
    DRAFT, ORDERED, PARTIALLY_RECEIVED, COMPLETED, CANCELLED
}
