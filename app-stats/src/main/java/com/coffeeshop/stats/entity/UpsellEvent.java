package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name="upsell_events")
public class UpsellEvent {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="rule_id",nullable=false)
    private UpsellRule rule;

    @Column(name="impression_id",nullable=false)
    private UUID impressionId;

    @Column(name="event_type",nullable=false,length=20)
    private String eventType;

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();
}
