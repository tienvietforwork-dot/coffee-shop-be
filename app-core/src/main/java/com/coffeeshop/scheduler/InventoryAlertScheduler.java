package com.coffeeshop.scheduler;

import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Periodically scans materials for low stock and pushes a websocket alert to
 * /topic/inventory-alerts. To avoid spamming clients on every run, we only
 * broadcast when a material's state CHANGES (newly below threshold, or has
 * recovered) - the last-alerted set is tracked in memory.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryAlertScheduler {

    private final MaterialRepository materialRepository;
    private final NotificationService notificationService;

    private final Set<Long> lastAlertedMaterialIds = new HashSet<>();

    @Scheduled(fixedRate = 5 * 60 * 1000L) // every 5 minutes
    public void scanLowStock() {
        List<Material> lowStock = materialRepository.findLowStock();
        Set<Long> currentlyLowIds = new HashSet<>();

        for (Material material : lowStock) {
            currentlyLowIds.add(material.getId());
            if (!lastAlertedMaterialIds.contains(material.getId())) {
                log.warn("Material '{}' dropped below threshold ({} < {})",
                        material.getName(), material.getQuantityInStock(), material.getMinThreshold());
                notificationService.notifyInventoryAlert(MaterialResponse.from(material));
            }
        }

        // Materials that recovered above threshold since the last scan are simply
        // dropped from the tracked set (no "recovered" notification is required
        // by the spec, but this keeps the set from growing unbounded).
        lastAlertedMaterialIds.clear();
        lastAlertedMaterialIds.addAll(currentlyLowIds);
    }
}
