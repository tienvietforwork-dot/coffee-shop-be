package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Data
@Schema(description = "Thông tin tạo hoặc cập nhật hồ sơ khách hàng")
public class CustomerRequest {
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    @Past
    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String gender;

    @Size(max = 10000)
    private String description;
}
