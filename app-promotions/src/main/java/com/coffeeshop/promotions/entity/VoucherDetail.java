package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "voucher_details")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE voucher_details SET del_flag = true WHERE id = ?")
public class VoucherDetail extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "incident_id", nullable = true)
    private Incident incident;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void onCreate() {
        super.onCreate();

        LocalDateTime now = LocalDateTime.now();
        if (this.issuedAt == null) {
            this.issuedAt = now;
        }

        if (this.expiresAt == null) {
            int validDays = (this.voucher != null && this.voucher.getValidDays() != null) ? this.voucher.getValidDays() : 30;
            this.expiresAt = this.issuedAt.plusDays(validDays);
        }

        if (this.status == null) {
            this.status = VoucherStatus.ISSUED;
        }
    }


}
