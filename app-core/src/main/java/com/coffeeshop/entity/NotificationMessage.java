package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One message on the outbox: queued in the order's transaction, sent by the job (ZNS first, SMS brandname as fallback).
 * {@code requestId} (order + event) keeps it from going twice; the provider also drops duplicates for 24 hours.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_messages")
public class NotificationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    @Column(name = "template_code", nullable = false, length = 30)
    private String templateCode;
    /** 84xxxxxxxxx */
    @Column(nullable = false, length = 15)
    private String phone;
    /** PENDING, SENT (provider accepted), DELIVERED (callback), FAILED */
    @Column(nullable = false, length = 15)
    private String status;
    /** ZNS or SMS — how it actually went */
    @Column(length = 5)
    private String channel;
    @Column(name = "request_id", nullable = false, length = 48, unique = true)
    private String requestId;
    /** ZNS template parameters, JSON */
    @Column(columnDefinition = "TEXT")
    private String params;
    /** the SMS text */
    @Column(length = 320)
    private String content;
    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;
    @Column(name = "provider_ref", length = 60)
    private String providerRef;
    @Column(length = 500)
    private String error;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
