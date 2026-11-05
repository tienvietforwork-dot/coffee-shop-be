package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Nguyên liệu: only a name and a unit; how much a coffee needs lives in its recipe (recipe_materials). No stock is kept. */
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
}
