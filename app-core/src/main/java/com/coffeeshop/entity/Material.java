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
@Table(name = "materials")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE materials SET del_flag = true WHERE id = ?")
public class Material extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 20)
    private String unit;
    /** Denormalised sum of remaining_quantity over this material's batches. */
    @Builder.Default
    @Column(name = "stock_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal minStock = BigDecimal.ZERO;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialStatus status = MaterialStatus.ACTIVE;

    public boolean isLowStock() {
        return stockQuantity.compareTo(minStock) < 0;
    }
}
