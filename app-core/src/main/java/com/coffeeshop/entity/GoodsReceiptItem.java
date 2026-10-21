package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dòng phiếu nhập. Quantities in the material's unit: {@code documentQuantity} as written on the supplier's delivery
 * note (SL theo chứng từ), {@code receivedQuantity} counted and accepted (SL thực nhập, becomes {@link #batch}),
 * {@code rejectedQuantity} refused at the door (hỏng, sai quy cách, quá hạn, không đạt nhiệt độ).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "goods_receipt_items")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE goods_receipt_items SET del_flag = true WHERE id = ?")
public class GoodsReceiptItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id")
    private PurchaseOrderItem purchaseOrderItem;
    /** the lot made from the accepted quantity; null when everything was refused */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MaterialBatch batch;
    /** packaging the counts were taken in (copied), e.g. "Hộp 1 L" × 1000 ml; null = loose units */
    @Column(name = "package_name", length = 50)
    private String packageName;
    @Column(name = "package_size", precision = 12, scale = 2)
    private BigDecimal packageSize;
    @Column(name = "document_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal documentQuantity;
    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedQuantity;
    @Builder.Default
    @Column(name = "rejected_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;
    /** đơn giá chưa thuế / đơn vị gốc */
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    /** nhiệt độ đo lúc nhận hàng mát / đông (°C) */
    @Column(precision = 5, scale = 1)
    private BigDecimal temperature;
    @Column(length = 255)
    private String note;

    /** thành tiền = SL thực nhập × đơn giá */
    public BigDecimal amount() {
        return unitPrice == null ? BigDecimal.ZERO : Money.vnd(unitPrice.multiply(receivedQuantity));
    }

    /** giao thiếu so với chứng từ (không tính phần trả lại) */
    public BigDecimal shortQuantity() {
        return documentQuantity.subtract(receivedQuantity).subtract(rejectedQuantity).max(BigDecimal.ZERO);
    }
}
