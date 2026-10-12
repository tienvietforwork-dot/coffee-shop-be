package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicFeedbackRequest {
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "0\\d{9,10}", message = "Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng 0")
    private String phone;

    @Size(max = 50)
    private String topic;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
