package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name="cross_sell_events",uniqueConstraints=@UniqueConstraint(columnNames={"impression_id","coffee_id","event_type"}))
public class CrossSellEvent {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="rule_id",nullable=false)
    private CrossSellRule rule;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="coffee_id",nullable=false)
    private Coffee coffee;

    @Column(name="event_type",nullable=false,length=20)
    private String eventType;

    @Column(name="impression_id",nullable=false,columnDefinition="uuid")
    private UUID impressionId;

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();
}
