package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name="upsell_rules")
public class UpsellRule {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,length=150)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="anchor_coffee_id",nullable=false)
    private Coffee anchorCoffee;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="upgrade_coffee_id",nullable=false)
    private Coffee upgradeCoffee;

    @Column(nullable=false,length=24)
    private String upgradeType="PREMIUM_VERSION";

    @Column(nullable=false)
    private int priority=0;

    @Column(name="min_cart_amount",nullable=false,precision=12,scale=2)
    private BigDecimal minCartAmount=BigDecimal.ZERO;

    @Column(name="starts_at")
    private LocalDateTime startsAt;

    @Column(name="ends_at")
    private LocalDateTime endsAt;

    @Column(nullable=false,length=16)
    private String status="DRAFT";

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
