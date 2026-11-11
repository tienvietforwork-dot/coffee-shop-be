package com.coffeeshop.promotions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
@SQLRestriction("del_flag = false")
public class Order extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 32)
    private String code;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "order_type", length = 20)
    private String orderType;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "voucher_code", length = 30)
    private String voucherCode;

    @Column(name = "voucher_discount", precision = 12, scale = 2)
    private BigDecimal voucherDiscount;
}
