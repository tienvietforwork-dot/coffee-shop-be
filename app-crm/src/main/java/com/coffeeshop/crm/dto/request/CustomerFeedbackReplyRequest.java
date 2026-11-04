package com.coffeeshop.crm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Nội dung khách hàng gửi tiếp trong luồng khiếu nại/góp ý của chính mình")
public class CustomerFeedbackReplyRequest {
    @NotBlank
    @Size(max = 10000)
    private String message;
}
