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
@Table(name = "permissions")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE permissions SET del_flag = true WHERE id = ?")
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Screen / action code, see com.coffeeshop.security.Perm. */
    @Column(nullable = false, length = 60)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    /** Frontend route of the screen; null for action-only permissions. */
    @Column(length = 100)
    private String path;
    @Column(nullable = false, length = 30)
    private String module;
    @Column(length = 255)
    private String description;
    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
