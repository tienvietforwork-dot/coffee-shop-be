package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionUpdateRequest {
    @Pattern(regexp = "CALL|CHAT|EMAIL|IN_STORE|NOTE")
    private String channel;
    @Size(max = 10000)
    private String content;

    private LocalDateTime occurredAt;

    @Size(max = 10000)
    private String outcome;
    @Size(max = 10000)
    private String note;
    @Positive
    private Long staffId;
    private LocalDateTime remindAt;
    private Boolean clearReminder;
    @Pattern(regexp = "OPEN|FOLLOW_UP|CLOSED")
    private String status;
}
