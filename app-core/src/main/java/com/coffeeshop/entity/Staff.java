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
@Table(name = "staff")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE staff SET del_flag = true WHERE id = ?")
public class Staff extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(length = 50)
    private String position;
    @Column(nullable = false, length = 15)
    private String phone;
    @Column(length = 100)
    private String email;
    @Column(name = "hire_date")
    private LocalDate hireDate;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false, length = 20)
    private WorkStatus workStatus = WorkStatus.ACTIVE;
}
