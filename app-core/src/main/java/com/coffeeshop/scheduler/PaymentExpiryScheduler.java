package com.coffeeshop.scheduler;

import com.coffeeshop.service.OnlinePaymentService;
import com.coffeeshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every minute: VNPAY links past their 15 minutes are closed and the QR / online orders still unpaid are cancelled. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final OnlinePaymentService onlinePaymentService;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.payment.expiry-scan-ms:60000}", initialDelay = 30_000)
    public void expireUnpaid() {
        for (Long orderId : onlinePaymentService.expire()) {
            try {
                orderService.cancelUnpaidOnline(orderId);
                log.info("Hủy đơn #{}: quá hạn thanh toán online", orderId);
            } catch (RuntimeException e) {
                log.warn("Không hủy được đơn #{} quá hạn thanh toán: {}", orderId, e.getMessage());
            }
        }
    }
}
