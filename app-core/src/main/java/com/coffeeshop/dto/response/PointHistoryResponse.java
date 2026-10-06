package com.coffeeshop.dto.response;

import com.coffeeshop.entity.LoyaltyPointHistory;

import java.time.LocalDateTime;

public record PointHistoryResponse(Long id, Integer pointsChange, String reason, String orderCode,
                                   LocalDateTime createdAt) {
    public static PointHistoryResponse from(LoyaltyPointHistory h) {
        return new PointHistoryResponse(h.getId(), h.getPointsChange(), h.getReason(),
                h.getOrder() == null ? null : h.getOrder().getOrderCode(), h.getCreatedAt());
    }
}
