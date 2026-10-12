package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Schema(title = "Nhóm khách hàng", description = "Nhóm thủ công hoặc được tính theo tiêu chí tự động")
@Table(name = "customer_groups")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE customer_groups SET del_flag = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerGroup extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_type", nullable = false, length = 20)
    private String groupType; // 'AUTOMATIC' hoặc 'MANUAL'

    // Ánh xạ trường JSONB từ PostgreSQL
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_rule", columnDefinition = "jsonb")
    private JsonNode criteriaRule;

    @Column(name = "recalculated_at")
    private LocalDateTime recalculatedAt;
}
