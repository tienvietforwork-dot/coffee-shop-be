package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Phường / xã có phí ship riêng ({@code fee}; null = theo tỉnh) hoặc không giao ({@code blocked}). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_wards")
public class DeliveryWard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private DeliveryProvince province;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(precision = 12, scale = 2)
    private BigDecimal fee;
    @Builder.Default
    @Column(nullable = false)
    private boolean blocked = false;
}
