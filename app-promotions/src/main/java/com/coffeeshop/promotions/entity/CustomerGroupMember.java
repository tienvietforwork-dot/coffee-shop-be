package com.coffeeshop.promotions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import javax.net.ssl.SSLSession;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_group_members")
@SQLRestriction("del_flag = false")
public class CustomerGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Builder.Default
    @Column(name = "is_manual", nullable = false)
    private Boolean isManual = false;

    @Builder.Default
    @Column(name = "is_excluded", nullable = false)
    private Boolean isExcluded = false;

    @Builder.Default
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();



}