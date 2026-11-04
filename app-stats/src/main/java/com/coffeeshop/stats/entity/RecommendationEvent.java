package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name="recommendation_events")
public class RecommendationEvent {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="rule_id",nullable=false)
    private RecommendationRule rule;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="coffee_id",nullable=false)
    private Coffee coffee;

    @Column(name="impression_id",nullable=false)
    private UUID impressionId;

    @Column(name="event_type",nullable=false,length=16)
    private String eventType;

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

}
