package com.coffeeshop.scheduler;

import com.coffeeshop.service.DeliveryFeeService;
import com.coffeeshop.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Mỗi phút: gói giao định kỳ sắp tới giờ giao thì tự tạo đơn giao (mỗi gói tối đa 1 đơn / ngày). */
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelayString = "${app.subscription.scan-ms:60000}", initialDelay = 45_000)
    public void createDueOrders() {
        LocalDateTime now = LocalDateTime.now(DeliveryFeeService.SHOP_ZONE);
        for (Long id : subscriptionService.due(now)) subscriptionService.runDue(id, now);
    }
}
