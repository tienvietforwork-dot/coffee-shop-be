package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.FeedbackResponse;
import java.time.LocalDateTime;

public record FeedbackResponseDto(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        Long feedbackId,
        Long staffId,
        String message,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
    public static FeedbackResponseDto from(FeedbackResponse response) {
        return new FeedbackResponseDto(
                response.getId(), response.getCreatedBy(), response.getCreatedAt(),
                response.getUpdatedBy(), response.getUpdatedAt(), response.getDelFlag(), response.getDelUser(),
                response.getFeedbackId(), response.getStaffId(), response.getMessage(), response.getSentAt(),
                response.getReadAt());
    }
}
