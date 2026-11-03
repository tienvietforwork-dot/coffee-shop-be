package com.coffeeshop.stats.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@Entity
@Table(name="cross_sell_rules")
public class CrossSellRule {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="anchor_coffee_id")
    private Coffee anchorCoffee;

    @Column(nullable=false)
    private int priority;

    @Column(name="min_cart_amount",nullable=false,precision=12,scale=2)
    private BigDecimal minCartAmount=BigDecimal.ZERO;

    @Column(name="starts_at")
    private LocalDateTime startsAt;

    @Column(name="ends_at")
    private LocalDateTime endsAt;

    @Column(nullable=false,length=20)
    private String status="DRAFT";

    @Column(name="created_by",nullable=false,length=50)
    private String createdBy="admin";

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(name="updated_by",length=50)
    private String updatedBy;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy="rule",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("position ASC, id ASC")
    private List<CrossSellItem> items = new ArrayList<>();
}
