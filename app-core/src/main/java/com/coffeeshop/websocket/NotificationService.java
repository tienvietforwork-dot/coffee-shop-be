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
 *  - /topic/tables             -> trạng thái bàn đổi bằng tay (sơ đồ bàn / quét QR)
 * Inside a transaction the message goes out only after it commits: clients refetch on it,
 * and a message sent before the commit makes them read the old data.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ORDERS_TOPIC = "/topic/orders";
    private static final String TABLES_TOPIC = "/topic/tables";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrderChanged(Object payload) {
        send(ORDERS_TOPIC, payload);
    }

    /** nhân viên đổi trạng thái bàn bằng tay: các màn sơ đồ bàn tải lại */
    public void notifyTablesChanged() {
        send(TABLES_TOPIC, java.util.Map.of("changed", true));
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
