package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Phiếu đặt hàng nhà cung cấp; see {@link PurchaseOrderStatus} for the life cycle. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchase_orders")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE purchase_orders SET del_flag = true WHERE id = ?")
public class PurchaseOrder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    /** Employee who drew up the order (from the logged-in account). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
    @Column(name = "expected_date")
    private LocalDate expectedDate;
    /** sent to the supplier */
    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;
    /** received in full, closed or cancelled */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(length = 255)
    private String note;
    /** closed short of the ordered quantities, or cancelled: why */
    @Column(name = "close_reason", length = 255)
    private String closeReason;
    /** điều khoản thanh toán: số ngày được nợ kể từ ngày nhận hàng (mặc định lấy của nhà cung cấp) */
    @Builder.Default
    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays = 0;
    /** giảm giá trên tổng tiền hàng */
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    /** thuế suất GTGT (%) của hàng mua, null = không có hóa đơn GTGT / không chịu thuế */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;
    /** chi phí nhập hàng (vận chuyển, bốc xếp) */
    @Builder.Default
    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /** tổng tiền hàng = Σ số lượng × đơn giá */
    public BigDecimal goodsAmount() {
        return items.stream().map(PurchaseOrderItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** cần trả nhà cung cấp = tiền hàng − giảm giá + thuế GTGT + chi phí nhập */
    public BigDecimal totalAmount() {
        return Money.payable(goodsAmount(), discountAmount, vatRate, shippingCost);
    }
    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
