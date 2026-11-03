package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supplier_materials")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE supplier_materials SET del_flag = true WHERE id = ?")
public class SupplierMaterial extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    /** packaging usually ordered from this supplier (null: loose, in the material's unit) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private MaterialPackage materialPackage;
    /** reference price per unit of the material (like material_batches.unit_cost) */
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
