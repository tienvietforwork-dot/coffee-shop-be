package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime registeredAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(), customer.getCreatedBy(), customer.getCreatedAt(),
                customer.getUpdatedBy(), customer.getUpdatedAt(), customer.getDelFlag(), customer.getDelUser(),
                customer.getFullName(), customer.getPhone(), customer.getEmail(), customer.getDateOfBirth(),
                customer.getGender(), customer.getDescription(), customer.getLoyaltyPoints(), customer.getRegisteredAt());
    }
}
