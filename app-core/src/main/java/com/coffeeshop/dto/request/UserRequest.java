package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Admin creates / edits an account. password: required on create, blank on edit = keep. */
public record UserRequest(
        @NotBlank @Size(max = 50) String username,
        @Size(max = 100) String password,
        @Size(max = 150) String fullName,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String phone,
        Boolean active,
        /** Role codes: ADMIN / STAFF / CUSTOMER. */
        @NotEmpty List<String> roles,
        Long staffId,
        Long customerId) {
}
