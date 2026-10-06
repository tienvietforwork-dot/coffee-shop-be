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
@Table(name = "recipes")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE recipes SET del_flag = true WHERE id = ?")
public class Recipe extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_id", nullable = false)
    private Coffee coffee;
    @Column(name = "brew_method", length = 30)
    private String brewMethod;
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "brew_time_min")
    private Integer brewTimeMin;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Builder.Default
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.PERSIST)
    private List<RecipeMaterial> materials = new ArrayList<>();
    @Builder.Default
    @OrderBy("stepNo ASC")
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.PERSIST)
    private List<RecipeStep> steps = new ArrayList<>();
}
