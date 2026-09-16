package com.coffeeshop.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around {@link SimpMessagingTemplate} so services don't need to
 * know STOMP destination names directly. Two topics are used:
 *  - /topic/orders             -> broadcast whenever an order is created/updated
 *  - /topic/inventory-alerts   -> broadcast when a material crosses below its min_threshold
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ORDERS_TOPIC = "/topic/orders";
    private static final String INVENTORY_ALERTS_TOPIC = "/topic/inventory-alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrderChanged(Object payload) {
        messagingTemplate.convertAndSend(ORDERS_TOPIC, payload);
    }

    public void notifyInventoryAlert(Object payload) {
        messagingTemplate.convertAndSend(INVENTORY_ALERTS_TOPIC, payload);
    }
}
