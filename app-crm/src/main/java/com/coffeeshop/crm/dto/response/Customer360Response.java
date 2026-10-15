package com.coffeeshop.crm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Customer360Response(
        ProfileDto profile,
        MetricsDto metrics,
        List<OrderDto> recentOrders,
        List<InteractionDto> recentInteractions,
        List<FeedbackDto> recentFeedbacks,
        List<TimelineEventDto> timeline
) {
    public record ProfileDto(Long id, String fullName, String phone, String email, java.time.LocalDate dateOfBirth,
                             String gender, String description,
                             Integer loyaltyPoints, LocalDateTime registeredAt,
                             List<AddressDto> addresses) { }
    public record AddressDto(Long id, String label, String recipientName, String recipientPhone,
                             String address, boolean isDefault) { }
    public record MetricsDto(BigDecimal totalSpend, long totalOrders, BigDecimal averageOrderValue,
                             long ordersLast30Days, LocalDateTime lastOrderAt,
                             String favoriteCoffee, String favoriteCategory) { }
    public record OrderDto(String orderCode, String status, BigDecimal totalAmount,
                           LocalDateTime orderedAt, String items) { }
    public record InteractionDto(Long id, String channel, String status, String content,
                                 String note, LocalDateTime remindAt, Long staffId,
                                 LocalDateTime occurredAt, String outcome, LocalDateTime createdAt) { }
    public record FeedbackDto(Long id, String topic, String issueType, String category, String sentiment,
                              String content, String status, boolean escalated,
                              String resolution, String satisfaction, Short rating, Long staffId,
                              List<String> topics, List<FeedbackReplyDto> responses, LocalDateTime createdAt) { }
    public record FeedbackReplyDto(Long id, String message, Long staffId, LocalDateTime sentAt) { }
    public record TimelineEventDto(String eventType, String description, LocalDateTime eventTime) { }
}
