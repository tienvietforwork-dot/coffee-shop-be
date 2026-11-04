package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.InteractionReminderNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InteractionReminderNotificationRepository extends JpaRepository<InteractionReminderNotification, Long> {
    @Modifying
    @Query(value = "insert into interaction_reminder_notifications (interaction_id, staff_id, remind_at) " +
            "values (:interactionId, :staffId, :remindAt) on conflict (interaction_id, remind_at) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("interactionId") Long interactionId, @Param("staffId") Long staffId,
                       @Param("remindAt") java.time.LocalDateTime remindAt);

    @Modifying
    @Query("update InteractionReminderNotification n set n.readAt = CURRENT_TIMESTAMP " +
            "where n.interactionId = :interactionId and n.readAt is null and n.delFlag = false")
    int acknowledgeOpenNotifications(@Param("interactionId") Long interactionId);
    Page<InteractionReminderNotification> findAllByDelFlagFalseOrderByRemindAtDesc(Pageable pageable);
    Page<InteractionReminderNotification> findAllByReadAtIsNullAndDelFlagFalseOrderByRemindAtAsc(Pageable pageable);
}
