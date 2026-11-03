package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierReturnSettlement;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiếu trả hàng nhập: goods of one phiếu nhập going back to the supplier (hỏng, sai quy cách, cận hạn…).
 * Completing it takes the quantities out of their lots. The supplier then lowers what the shop owes
 * ({@link SupplierReturnSettlement#DEBT}) or pays the money back ({@link SupplierReturnSettlement#REFUND}),
 * and normally issues a hóa đơn điều chỉnh giảm, whose number is kept here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supplier_returns")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE supplier_returns SET del_flag = true WHERE id = ?")
public class SupplierReturn extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierReturnStatus status = SupplierReturnStatus.DRAFT;
    @Column(nullable = false, length = 255)
    private String reason;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierReturnSettlement settlement = SupplierReturnSettlement.DEBT;
    /** REFUND: how the supplier paid the money back */
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", length = 20)
    private PaymentMethod refundMethod;
    /** thuế suất GTGT của hàng trả (theo phiếu nhập gốc) */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;
    /** số hóa đơn điều chỉnh giảm nhà cung cấp lập */
    @Column(name = "adjustment_invoice_no", length = 30)
    private String adjustmentInvoiceNo;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<SupplierReturnItem> items = new ArrayList<>();

    public BigDecimal goodsAmount() {
        return items.stream().map(SupplierReturnItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** giá trị hàng trả gồm thuế GTGT */
    public BigDecimal totalAmount() {
        return Money.payable(goodsAmount(), BigDecimal.ZERO, vatRate, BigDecimal.ZERO);
    }
}
