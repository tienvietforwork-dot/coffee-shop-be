package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Nhật ký thao tác: one write request to the API (thêm / sửa / xóa / thao tác nghiệp vụ), who did it and the result.
 * Written in batches from a queue (see AuditLogService), never edited.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 50)
    private String username;
    /** first path segment after /api: orders, purchase-orders, cash-shifts… */
    @Column(nullable = false, length = 50)
    private String module;
    /** CREATE, UPDATE, DELETE or the action word of the path (confirm, cancel, receipts…) */
    @Column(nullable = false, length = 40)
    private String action;
    @Column(nullable = false, length = 10)
    private String method;
    @Column(nullable = false, length = 255)
    private String path;
    @Column(name = "entity_id", length = 40)
    private String entityId;
    @Column(name = "status_code", nullable = false)
    private int statusCode;
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;
    @Column(length = 64)
    private String ip;
    /** request body, passwords masked, cut to 1000 characters */
    @Column(columnDefinition = "TEXT")
    private String detail;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
