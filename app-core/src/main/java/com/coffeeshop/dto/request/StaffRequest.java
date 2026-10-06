package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.WorkStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Hồ sơ nhân sự. Quyền truy cập hệ thống nằm ở tài khoản (users + role), không ở đây. */
public record StaffRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 50) String position,
        @NotBlank @Size(max = 15) String phone,
        @Email @Size(max = 100) String email,
        LocalDate hireDate,
        WorkStatus workStatus) {
}
