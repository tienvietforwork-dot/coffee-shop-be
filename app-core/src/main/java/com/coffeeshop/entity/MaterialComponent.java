package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** One line of a prepared material's formula: one batch (material.yieldQuantity) needs `quantity` of `component`. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "material_components")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE material_components SET del_flag = true WHERE id = ?")
public class MaterialComponent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Material component;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
}
