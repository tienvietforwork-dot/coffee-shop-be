package com.coffeeshop.crm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Stable response contracts for CRM analytics endpoints. */
public final class CustomerInsightResponses {
    private CustomerInsightResponses() { }

    public record CustomerMetricResponse(Long customerId, String fullName, String phone,
                                         BigDecimal totalSpend, long totalOrders,
                                         BigDecimal averageOrderValue, LocalDateTime lastOrderAt,
                                         Integer daysSinceLastOrder) { }

    public record GroupMetricResponse(Long groupId, String groupName, long memberCount,
                                      BigDecimal totalSpend, long totalOrders,
                                      BigDecimal averageOrderValue, String favoriteCategory,
                                      String favoriteCoffee, long interactionCount, long feedbackCount) { }

    public record CustomerValueResponse(Long customerId, String fullName, String phone, long loyaltyPoints,
                                        LocalDateTime registeredAt, BigDecimal totalSpend, long totalOrders,
                                        BigDecimal averageOrderValue, LocalDateTime lastOrderAt,
                                        Integer daysSinceLastOrder, Integer customerTenureDays) { }

    public record ProductAffinityResponse(Long itemId, String itemName, long customerCount, long orderCount,
                                          long quantitySold, BigDecimal revenue) { }

    public record SalesTrendResponse(LocalDateTime month, long totalOrders, long activeCustomers,
                                     BigDecimal revenue) { }

    public record FeedbackSummaryResponse(String issueType, String category, String sentiment, String status,
                                          long feedbackCount, long customerCount) { }

    public record CustomerGrowthResponse(LocalDateTime month, long newProfiles, long activeCustomers,
                                         long firstTimeBuyers, long returningCustomers) { }

    public record RepeatedComplaintResponse(String topic, long complaintCount, long customerCount) { }

    public record PeriodMetricsResponse(BigDecimal totalSpend, long totalOrders,
                                        BigDecimal averageOrderValue, LocalDateTime lastOrderAt) { }

    public record PeriodComparisonResponse(Long customerId, PeriodMetricsResponse firstPeriod,
                                           PeriodMetricsResponse secondPeriod) { }
}
