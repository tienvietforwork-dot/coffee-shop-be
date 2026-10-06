package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "incidents")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE incidents SET del_flag = true WHERE id = ?")
public class Incident extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    /** Employee who resolved it (from the logged-in account). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private Staff handledBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentType type;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentSeverity severity;
    @Column(length = 255)
    private String reason;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String resolution;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;
    @Builder.Default
    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
