package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Một khoảng km của option giá: quãng đường trong (fromKm, toKm] → phí. Xa hơn khoảng cuối = ngoài phạm vi giao. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_fee_tiers")
public class DeliveryFeeTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private DeliveryFeePlan plan;
    @Column(name = "from_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal fromKm;
    @Column(name = "to_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal toKm;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;

    public String label() {
        return fromKm.stripTrailingZeros().toPlainString() + "–" + toKm.stripTrailingZeros().toPlainString() + " km";
    }
}
