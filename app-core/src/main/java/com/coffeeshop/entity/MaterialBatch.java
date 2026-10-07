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
@Table(name = "material_batches")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE material_batches SET del_flag = true WHERE id = ?")
public class MaterialBatch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @Column(name = "import_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal importQuantity;
    @Column(name = "remaining_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingQuantity;
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status = BatchStatus.AVAILABLE;
    /** PREPARING batch: when it is expected to be ready */
    @Column(name = "ready_at")
    private LocalDateTime readyAt;
    /** prepared-material lot: exact expiry (expiry_date keeps the day, for FEFO and alerts) */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
