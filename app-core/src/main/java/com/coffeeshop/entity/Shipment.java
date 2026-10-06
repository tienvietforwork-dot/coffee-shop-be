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
@Table(name = "shipments")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE shipments SET del_flag = true WHERE id = ?")
public class Shipment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private CustomerAddress address;
    @Column(length = 50)
    private String carrier;
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;
    @Builder.Default
    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.PENDING;
    @Column(name = "booked_at")
    private LocalDateTime bookedAt;
    @Column(name = "driver_accepted_at")
    private LocalDateTime driverAcceptedAt;
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    @Column(columnDefinition = "TEXT")
    private String note;
}
