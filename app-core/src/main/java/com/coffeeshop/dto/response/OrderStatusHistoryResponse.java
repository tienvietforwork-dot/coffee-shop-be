package com.coffeeshop.dto.response;

import com.coffeeshop.entity.OrderStatusHistory;
import com.coffeeshop.entity.enums.OrderStatus;

import java.time.LocalDateTime;

/** {@code by}: the staff member's name, otherwise the account (customer username, "guest", "system"). */
public record OrderStatusHistoryResponse(Long id, OrderStatus fromStatus, OrderStatus toStatus, String by,
                                         String note, LocalDateTime at) {

    public static OrderStatusHistoryResponse from(OrderStatusHistory h) {
        return new OrderStatusHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getStaff() != null ? h.getStaff().getFullName() : h.getCreatedBy(), h.getNote(), h.getCreatedAt());
    }
}
