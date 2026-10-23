package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.InteractionReminderNotification;
import java.time.LocalDateTime;

public record InteractionReminderNotificationResponse(Long id, Long interactionId, Long staffId,
        LocalDateTime remindAt, LocalDateTime readAt) {
    public static InteractionReminderNotificationResponse from(InteractionReminderNotification value) {
        return new InteractionReminderNotificationResponse(value.getId(), value.getInteractionId(),
                value.getStaffId(), value.getRemindAt(), value.getReadAt());
    }
}
