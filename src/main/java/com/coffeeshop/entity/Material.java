package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "quantity_in_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityInStock;

    @Column(name = "min_threshold", nullable = false, precision = 14, scale = 3)
    private BigDecimal minThreshold;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    public boolean isBelowThreshold() {
        return quantityInStock != null && minThreshold != null
                && quantityInStock.compareTo(minThreshold) < 0;
    }
}
