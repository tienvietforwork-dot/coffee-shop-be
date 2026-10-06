package com.coffeeshop.scheduler;

import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.service.InventoryService;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * - every 5 minutes: push a websocket alert for materials newly below min_stock;
 * - every night: write off expired batches (FEFO keeps this rare).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryScheduler {

    private final MaterialRepository materialRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    private final Set<Long> lastAlerted = new HashSet<>();

    @Scheduled(fixedRate = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void scanLowStock() {
        Set<Long> current = new HashSet<>();
        for (var material : materialRepository.findLowStock()) {
            current.add(material.getId());
            if (!lastAlerted.contains(material.getId())) {
                notificationService.notifyInventoryAlert(MaterialResponse.from(material));
            }
        }
        lastAlerted.clear();
        lastAlerted.addAll(current);
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void expireBatches() {
        int expired = inventoryService.expireBatches();
        if (expired > 0) log.info("Wrote off {} expired material batches", expired);
    }
}
