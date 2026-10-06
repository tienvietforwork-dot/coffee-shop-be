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
@Table(name = "dining_tables")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE dining_tables SET del_flag = true WHERE id = ?")
public class DiningTable extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "table_no", nullable = false, length = 10)
    private String tableNo;
    @Column(name = "qr_code", nullable = false, length = 255)
    private String qrCode;
    @Column(length = 50)
    private String area;
    private Integer capacity;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;
}
