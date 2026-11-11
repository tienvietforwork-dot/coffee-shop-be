package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Nguyên liệu dạng thô: tên, đơn vị và số lượng hiện có. Định lượng mỗi ly nằm ở công thức (recipe_materials);
 * bắt đầu pha một đơn thì tự trừ, món thiếu nguyên liệu tự "Hết món" (StockService).
 */
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
    /** số lượng hiện có, theo {@link #unit} */
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal quantity = java.math.BigDecimal.ZERO;
}
