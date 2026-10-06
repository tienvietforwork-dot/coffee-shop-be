package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.enums.WorkStatus;

import java.time.LocalDate;

public record StaffResponse(Long id, String fullName, String position, String phone, String email,
                            LocalDate hireDate, WorkStatus workStatus) {
    public static StaffResponse from(Staff s) {
        return new StaffResponse(s.getId(), s.getFullName(), s.getPosition(), s.getPhone(), s.getEmail(),
                s.getHireDate(), s.getWorkStatus());
    }
}
