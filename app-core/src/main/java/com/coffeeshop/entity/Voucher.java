package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.VoucherDiscountType;
import com.coffeeshop.entity.enums.VoucherTypeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Loại voucher: giá trị giảm, đơn tối thiểu và {@code quantity} = số lượt được dùng tối đa.
 * Mỗi voucher phát cho khách là một {@link VoucherDetail}; voucher còn hạn chưa dùng giữ 1 lượt,
 * đã dùng chiếm luôn, hết hạn / thu hồi trả lượt.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vouchers")
// No @SQLRestriction: app-promotions soft-deletes voucher types whose issued vouchers (voucher_details) stay on orders
// and incidents; the issuing queries filter del_flag themselves.
@SQLDelete(sql = "UPDATE vouchers SET del_flag = true WHERE id = ?")
public class Voucher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;
    /** How discount_value is read; null on older rows = FIXED_AMOUNT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 15)
    private VoucherDiscountType discountType;
    /** Order subtotal needed to use the voucher. */
    @Builder.Default
    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    /** Số lượt được dùng tối đa (gồm cả lượt đang giữ chỗ) — kiểu cũ; app-promotions now counts this down as stock. */
    private Integer quantity;
    /** Số lượt tối đa do app-promotions đặt; khi có thì thay cho {@link #quantity}. */
    @Column(name = "max_quantity")
    private Integer maxQuantity;
    /** Hạn mặc định khi phát, tính từ lúc phát; null = không hết hạn. */
    @Column(name = "valid_days")
    private Integer validDays;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherTypeStatus status = VoucherTypeStatus.ACTIVE;

    public boolean isPercentage() {
        return discountType == VoucherDiscountType.PERCENTAGE;
    }

    /** What this voucher takes off {@code amount}: a percent of it (rounded to whole đồng) or a fixed sum, never more than the amount. */
    public BigDecimal discountOn(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || discountValue == null) return BigDecimal.ZERO;
        BigDecimal off = isPercentage()
                ? amount.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : discountValue;
        return off.min(amount);
    }

    /** Số lượt tối đa của loại voucher: max_quantity, chưa có thì quantity. */
    public int slots() {
        if (maxQuantity != null) return maxQuantity;
        return quantity == null ? 0 : quantity;
    }
}
