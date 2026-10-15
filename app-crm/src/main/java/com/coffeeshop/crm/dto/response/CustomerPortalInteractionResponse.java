package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.Interaction;

import java.time.LocalDateTime;

/** Only fields deliberately marked visible to the customer are returned. */
public record CustomerPortalInteractionResponse(
        Long id,
        String channel,
        String content,
        LocalDateTime occurredAt,
        String status,
        String outcome
) {
    public static CustomerPortalInteractionResponse from(Interaction interaction) {
        return new CustomerPortalInteractionResponse(interaction.getId(), interaction.getChannel(),
                interaction.getContent(), interaction.getOccurredAt(), interaction.getStatus(), interaction.getOutcome());
    }
}
