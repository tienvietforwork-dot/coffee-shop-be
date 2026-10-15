package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** How a material is bought: "Hộp 1 L" holds {@code size} of the material's unit (e.g. 1000 ml). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "material_packages")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE material_packages SET del_flag = true WHERE id = ?")
public class MaterialPackage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal size;
}
