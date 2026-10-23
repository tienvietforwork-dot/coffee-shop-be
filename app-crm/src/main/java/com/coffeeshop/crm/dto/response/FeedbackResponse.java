package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Feedback;
import com.coffeeshop.crm.entity.Customer;
import java.time.LocalDateTime;
import java.util.Set;

public record FeedbackResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        Long customerId,
        String customerName,
        String customerPhone,
        Long staffId,
        String topic,
        Set<String> topics,
        String issueType,
        String category,
        String sentiment,
        String content,
        String status,
        Boolean escalated,
        String resolution,
        String satisfaction,
        Short rating,
        LocalDateTime closedAt,
        String approvalStatus,
        String approvalNote,
        String approvedBy,
        LocalDateTime approvedAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return from(feedback, null);
    }

    public static FeedbackResponse from(Feedback feedback, Customer customer) {
        return new FeedbackResponse(
                feedback.getId(), feedback.getCreatedBy(), feedback.getCreatedAt(),
                feedback.getUpdatedBy(), feedback.getUpdatedAt(), feedback.getDelFlag(), feedback.getDelUser(),
                feedback.getCustomerId(), customer == null ? null : customer.getFullName(),
                customer == null ? null : customer.getPhone(), feedback.getStaffId(), feedback.getTopic(), feedback.getTopics(),
                feedback.getIssueType(), feedback.getCategory(), feedback.getSentiment(), feedback.getContent(),
                feedback.getStatus(), feedback.getEscalated(), feedback.getResolution(), feedback.getSatisfaction(),
                feedback.getRating(), feedback.getClosedAt(), feedback.getApprovalStatus(), feedback.getApprovalNote(),
                feedback.getApprovedBy(), feedback.getApprovedAt());
    }
}
