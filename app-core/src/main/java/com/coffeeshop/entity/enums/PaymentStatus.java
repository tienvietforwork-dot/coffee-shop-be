package com.coffeeshop.entity.enums;

/**
 * REFUND_PENDING: đơn bị từ chối / hủy sau khi khách đã trả online nhưng cổng thanh toán không hoàn tiền được —
 * nhân viên chuyển khoản trả khách rồi đánh dấu đã hoàn (ghi mã giao dịch).
 */
public enum PaymentStatus {
    PENDING, PAID, FAILED, REFUNDED, REFUND_PENDING
}
