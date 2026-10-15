package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Interaction;
import java.time.LocalDateTime;

public record InteractionResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        Long customerId,
        String channel,
        String content,
        LocalDateTime occurredAt,
        String note,
        Long staffId,
        String status,
        LocalDateTime remindAt,
        LocalDateTime closedAt,
        String outcome,
        Boolean visibleToCustomer
) {
    public static InteractionResponse from(Interaction interaction) {
        return new InteractionResponse(
                interaction.getId(), interaction.getCreatedBy(), interaction.getCreatedAt(),
                interaction.getUpdatedBy(), interaction.getUpdatedAt(), interaction.getDelFlag(), interaction.getDelUser(),
                interaction.getCustomerId(), interaction.getChannel(), interaction.getContent(), interaction.getOccurredAt(),
                interaction.getNote(), interaction.getStaffId(), interaction.getStatus(), interaction.getRemindAt(),
                interaction.getClosedAt(), interaction.getOutcome(), interaction.getVisibleToCustomer());
    }
}
