package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Customer self sign-up; the phone number becomes the username. */
public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
        @NotBlank @Size(min = 6, max = 100, message = "Mật khẩu tối thiểu 6 ký tự") String password,
        @Size(max = 100) String fullName,
        @Email @Size(max = 100) String email) {
}
