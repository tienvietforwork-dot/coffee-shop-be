package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Lịch sử chuyến giao: every status change, from staff or from the partner's webhook (raw payload kept). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    /** STAFF, PARTNER or SYSTEM */
    @Column(nullable = false, length = 10)
    private String source;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShipmentStatus status;
    @Column(name = "partner_status", length = 30)
    private String partnerStatus;
    @Column(length = 255)
    private String note;
    @Column(columnDefinition = "TEXT")
    private String raw;
    @Column(name = "created_by", length = 50)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
