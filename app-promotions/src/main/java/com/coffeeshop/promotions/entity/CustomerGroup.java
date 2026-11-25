package com.coffeeshop.promotions.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_groups")
@SQLRestriction("del_flag = false")
public class CustomerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_type", nullable = false, length = 20)
    private String groupType;

    @Column(name = "classification_code", length = 30)
    private String classificationCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_rule", columnDefinition = "jsonb")
    private JsonNode criteriaRule;

    @Column(name = "recalculated_at")
    private LocalDateTime recalculatedAt;
}