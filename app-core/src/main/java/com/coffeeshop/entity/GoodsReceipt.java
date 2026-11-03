package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiếu nhập kho (mẫu 01-VT): one delivery received, against a purchase order or loose (purchaseOrder null).
 * Lines keep the quantity on the supplier's paper next to what was actually taken in; accepted goods become lots.
 * Not edited once saved; it can be cancelled (hủy) while none of its lots has been used.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "goods_receipts")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE goods_receipts SET del_flag = true WHERE id = ?")
public class GoodsReceipt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;
    /** người nhập (nhân viên đăng nhập) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    /** họ tên người giao hàng (nhân viên / tài xế của nhà cung cấp) */
    @Column(name = "deliverer_name", length = 100)
    private String delivererName;
    /** "Theo hóa đơn số … ngày …": ký hiệu, số, ngày hóa đơn của nhà cung cấp (nếu có) */
    @Column(name = "invoice_series", length = 20)
    private String invoiceSeries;
    @Column(name = "invoice_no", length = 20)
    private String invoiceNo;
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    /** ảnh phiếu giao hàng / hóa đơn kèm theo (images.id) */
    @Column(name = "document_image_id")
    private Long documentImageId;
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;
    /** chi phí nhập hàng (vận chuyển, bốc xếp) */
    @Builder.Default
    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;
    /** hạn thanh toán = ngày nhập + số ngày được nợ */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoodsReceiptStatus status = GoodsReceiptStatus.COMPLETED;
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<GoodsReceiptItem> items = new ArrayList<>();

    /** tổng tiền hàng thực nhập = Σ SL thực nhập × đơn giá */
    public BigDecimal goodsAmount() {
        return items.stream().map(GoodsReceiptItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** cần trả nhà cung cấp; 0 once cancelled */
    public BigDecimal payable() {
        if (status == GoodsReceiptStatus.CANCELLED) return BigDecimal.ZERO;
        return Money.payable(goodsAmount(), discountAmount, vatRate, shippingCost);
    }
}
