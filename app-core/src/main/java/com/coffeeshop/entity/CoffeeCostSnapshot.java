package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Giá vốn of a coffee at a point in time (written by the job when it changes): cost, selling price, food cost %. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coffee_cost_snapshots")
public class CoffeeCostSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_id", nullable = false)
    private Coffee coffee;
    @Column(name = "recipe_id")
    private Long recipeId;
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
