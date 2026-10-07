package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/** One run of a background service (like A_BACKGROUND_SERVICE in MES); created_by = who triggered it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "background_service_logs")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE background_service_logs SET del_flag = true WHERE id = ?")
public class BackgroundServiceLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String service;
    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;
    @Column(name = "duration_ms")
    private Integer durationMs;
    @Column(columnDefinition = "TEXT")
    private String msg;
    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;
}
