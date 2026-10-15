package com.coffeeshop.crm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Thông tin nhóm khách hàng; nhóm thủ công không gửi criteriaRule")
public class CustomerGroupRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotBlank
    @Pattern(regexp = "^(AUTOMATIC|MANUAL)$")
    private String groupType;

    @Valid
    private CriteriaRuleDTO criteriaRule;
}
