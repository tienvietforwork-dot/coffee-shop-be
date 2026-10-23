package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Nội dung trả lời trong luồng khiếu nại/góp ý của khách hàng")
public class FeedbackResponseRequest {
    @NotBlank
    @Size(max = 10000)
    private String message;

    @Positive
    private Long staffId;
}
