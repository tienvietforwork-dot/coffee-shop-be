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
@Table(name = "customer_addresses")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE customer_addresses SET del_flag = true WHERE id = ?")
public class CustomerAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Column(length = 30)
    private String label;
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 15)
    private String recipientPhone;
    @Column(nullable = false, length = 255)
    private String address;
    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;
}
