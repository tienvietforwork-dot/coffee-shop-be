package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_subscription_items")
public class DeliverySubscriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private DeliverySubscription subscription;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_id", nullable = false)
    private Coffee coffee;
    @Column(nullable = false)
    private Integer quantity;
    @Column(length = 255)
    private String note;
}
