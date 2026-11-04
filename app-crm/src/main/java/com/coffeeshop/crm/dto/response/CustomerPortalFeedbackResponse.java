package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Feedback;
import com.coffeeshop.crm.entity.FeedbackResponse;

import java.time.LocalDateTime;
import java.util.List;

/** Customer-safe view of the customer's own feedback and staff replies. */
public record CustomerPortalFeedbackResponse(
        Long id,
        String topic,
        String issueType,
        String category,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<Reply> replies
) {
    public record Reply(Long id, String senderType, String message, LocalDateTime sentAt, LocalDateTime readAt) { }

    public static CustomerPortalFeedbackResponse from(Feedback feedback, List<FeedbackResponse> replies) {
        return new CustomerPortalFeedbackResponse(
                feedback.getId(), feedback.getTopic(), feedback.getIssueType(), feedback.getCategory(),
                feedback.getContent(), feedback.getStatus(), feedback.getCreatedAt(), feedback.getClosedAt(),
                replies.stream().map(reply -> new Reply(reply.getId(), reply.getSenderType(), reply.getMessage(),
                        reply.getSentAt(), reply.getReadAt())).toList());
    }
}
