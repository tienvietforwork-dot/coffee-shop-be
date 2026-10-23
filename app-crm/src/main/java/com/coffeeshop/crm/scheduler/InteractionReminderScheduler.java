package com.coffeeshop.crm.scheduler;

import com.coffeeshop.crm.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionReminderScheduler {
    private final InteractionService interactionService;

    /** Finds due follow-ups each minute and stores one persistent in-app notification per reminder time. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void dispatchDueReminders() {
        int created = interactionService.dispatchDueReminderNotifications();
        if (created > 0) log.info("Đã tạo {} thông báo nhắc chăm sóc đến hạn", created);
    }
}
