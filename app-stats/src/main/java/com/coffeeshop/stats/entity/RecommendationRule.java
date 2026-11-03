package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name="recommendation_rules")
public class RecommendationRule {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,length=150)
    private String name;

    @Column(nullable=false,length=24)
    private String type="PRODUCT";

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="anchor_coffee_id")
    private Coffee anchorCoffee;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="target_coffee_id")
    private Coffee targetCoffee;

    @Column(nullable=false)
    private int priority=0;

    @Column(nullable=false,length=16)
    private String status="DRAFT";

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
