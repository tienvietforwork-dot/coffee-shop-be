package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CustomerResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        String fullName,
        String phone,
        String email,
        LocalDate dateOfBirth,
        String gender,
        String description,
        Integer loyaltyPoints,
        LocalDateTime registeredAt,
        List<String> groups,
        long orderCount,
        BigDecimal totalSpent,
        LocalDateTime lastOrderAt
) {
    public static CustomerResponse from(Customer customer) {
        return from(customer, List.of(), new com.coffeeshop.crm.repository.CustomerAnalyticsRepository.CustomerListMetrics(0, BigDecimal.ZERO, null));
    }

    public static CustomerResponse from(Customer customer, List<String> groups) {
        return from(customer, groups, new com.coffeeshop.crm.repository.CustomerAnalyticsRepository.CustomerListMetrics(0, BigDecimal.ZERO, null));
    }

    public static CustomerResponse from(Customer customer, List<String> groups,
                                        com.coffeeshop.crm.repository.CustomerAnalyticsRepository.CustomerListMetrics metrics) {
        return new CustomerResponse(
                customer.getId(), customer.getCreatedBy(), customer.getCreatedAt(),
                customer.getUpdatedBy(), customer.getUpdatedAt(), customer.getDelFlag(), customer.getDelUser(),
                customer.getFullName(), customer.getPhone(), customer.getEmail(), customer.getDateOfBirth(),
                customer.getGender(), customer.getDescription(), customer.getLoyaltyPoints(), customer.getRegisteredAt(), groups,
                metrics.orderCount(), metrics.totalSpent(), metrics.lastOrderAt());
    }
}
