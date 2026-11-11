package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackApprovalRequest {
    @NotBlank
    @Pattern(regexp = "APPROVE|REJECT")
    private String decision;

    @Size(max = 2000)
    private String note;
}
