package com.coffeeshop.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "interaction_reminder_notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"interaction_id", "remind_at"}))
@Getter @Setter
public class InteractionReminderNotification extends BaseEntity {
    @Column(name = "interaction_id", nullable = false)
    private Long interactionId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
