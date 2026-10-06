package com.coffeeshop.scheduler;

import com.coffeeshop.entity.enums.CartStatus;
import com.coffeeshop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Open carts untouched for a day are marked "bỏ dở" (ABANDONED). */
@Component
@Slf4j
@RequiredArgsConstructor
public class CartCleanupScheduler {

    private final CartRepository cartRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void abandonStaleCarts() {
        int n = cartRepository.markStale(CartStatus.OPEN, CartStatus.ABANDONED, LocalDateTime.now().minusDays(1));
        if (n > 0) log.info("Marked {} carts as abandoned", n);
    }
}
