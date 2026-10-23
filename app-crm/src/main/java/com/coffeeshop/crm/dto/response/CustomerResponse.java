package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        List<String> groups
) {
    public static CustomerResponse from(Customer customer) {
        return from(customer, List.of());
    }

    public static CustomerResponse from(Customer customer, List<String> groups) {
        return new CustomerResponse(
                customer.getId(), customer.getCreatedBy(), customer.getCreatedAt(),
                customer.getUpdatedBy(), customer.getUpdatedAt(), customer.getDelFlag(), customer.getDelUser(),
                customer.getFullName(), customer.getPhone(), customer.getEmail(), customer.getDateOfBirth(),
                customer.getGender(), customer.getDescription(), customer.getLoyaltyPoints(), customer.getRegisteredAt(), groups);
    }
}
