package com.coffeeshop.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Thin wrapper around {@link SimpMessagingTemplate} so services don't need to
 * know STOMP destination names directly. Topics:
 *  - /topic/orders             -> broadcast whenever an order is created/updated
 *  - /topic/inventory-alerts   -> broadcast when a material crosses below its min_threshold
 *  - /topic/menu               -> coffees switched sold out / back on by stock
 * Inside a transaction the message goes out only after it commits: clients refetch on it,
 * and a message sent before the commit makes them read the old data.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ORDERS_TOPIC = "/topic/orders";
    private static final String INVENTORY_ALERTS_TOPIC = "/topic/inventory-alerts";
    private static final String MENU_TOPIC = "/topic/menu";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrderChanged(Object payload) {
        send(ORDERS_TOPIC, payload);
    }

    public void notifyInventoryAlert(Object payload) {
        send(INVENTORY_ALERTS_TOPIC, payload);
    }

    /** Coffees whose status was switched by stock (StockService.syncCoffeeStatus). */
    public void notifyMenuChanged(Object payload) {
        send(MENU_TOPIC, payload);
    }

    private void send(String topic, Object payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messagingTemplate.convertAndSend(topic, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(topic, payload);
            }
        });
    }
}
