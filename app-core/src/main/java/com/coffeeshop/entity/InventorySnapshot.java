package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tồn kho chốt cuối ngày of one material (job at 23:55): quantity and value (Σ lots × unit cost). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_snapshots")
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;
    @Column(name = "stock_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal value;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
