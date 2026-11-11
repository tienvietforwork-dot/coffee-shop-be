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
 * Inside a transaction the message goes out only after it commits: clients refetch on it,
 * and a message sent before the commit makes them read the old data.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ORDERS_TOPIC = "/topic/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrderChanged(Object payload) {
        send(ORDERS_TOPIC, payload);
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
