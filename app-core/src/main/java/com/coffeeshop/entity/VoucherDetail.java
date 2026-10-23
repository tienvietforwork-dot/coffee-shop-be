package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/** Một voucher đã phát thuộc một {@link Voucher}: mã riêng, phát lúc nào, hết hạn lúc nào, dùng ở đơn nào. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "voucher_details")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE voucher_details SET del_flag = true WHERE id = ?")
public class VoucherDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
    @Column(nullable = false, length = 30)
    private String code;
    /** Owner; null = shared voucher anyone can use once. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    /** Order the voucher was redeemed on. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    /** Incident this voucher compensates, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherStatus status = VoucherStatus.ISSUED;
    @Builder.Default
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
