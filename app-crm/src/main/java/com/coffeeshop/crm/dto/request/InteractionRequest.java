package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(description = "Thông tin ghi nhận một lần tương tác với khách hàng")
public class InteractionRequest {
    @NotNull
    @Positive
    private Long customerId;
    @NotBlank
    @Pattern(regexp = "CALL|CHAT|EMAIL|IN_STORE|NOTE")
    private String channel;
    @Size(max = 10000)
    private String content;

    private LocalDateTime occurredAt;
    @Size(max = 10000)
    private String note;
    @Positive
    private Long staffId;
    private LocalDateTime remindAt;
    @Schema(description = "Đặt true nếu nội dung tương tác phù hợp để khách xem trong tài khoản")
    private Boolean visibleToCustomer = false;
}
