package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Role;
import com.coffeeshop.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String username, String fullName, String email, String phone, boolean active,
                           List<String> roles, Long staffId, String staffName, Long customerId, String customerName,
                           LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(), u.isActive(),
                u.getRoles().stream().map(Role::getCode).toList(),
                u.getStaff() == null ? null : u.getStaff().getId(),
                u.getStaff() == null ? null : u.getStaff().getFullName(),
                u.getCustomer() == null ? null : u.getCustomer().getId(),
                u.getCustomer() == null ? null : u.getCustomer().getFullName(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getCreatedBy());
    }
}
