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
@Table(name = "roles")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE roles SET del_flag = true WHERE id = ?")
public class Role extends BaseEntity {
    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";
    public static final String CUSTOMER = "CUSTOMER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** ADMIN (Quản lý) / STAFF (Nhân viên) / CUSTOMER (Khách hàng) – the only three roles. */
    @Column(nullable = false, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 255)
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = CascadeType.PERSIST)
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
