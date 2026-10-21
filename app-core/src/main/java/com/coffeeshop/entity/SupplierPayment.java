package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.SupplierPaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phiếu chi trả nhà cung cấp (mẫu 02-TT): tiền mặt or chuyển khoản (reference = mã giao dịch).
 * {@code goodsReceipt}: paid on that receipt (applied to it first); {@code purchaseOrder}: trả trước / đặt cọc.
 * Otherwise it pays the oldest open receipts first. Not edited; cancelled with a reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supplier_payments")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE supplier_payments SET del_flag = true WHERE id = ?")
public class SupplierPayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id")
    private GoodsReceipt goodsReceipt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;
    /** mã giao dịch chuyển khoản / số ủy nhiệm chi */
    @Column(length = 100)
    private String reference;
    /** họ tên người nhận tiền */
    @Column(name = "payee_name", length = 100)
    private String payeeName;
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
    /** người lập phiếu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    /** ảnh chứng từ chuyển khoản / giấy biên nhận (images.id) */
    @Column(name = "document_image_id")
    private Long documentImageId;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierPaymentStatus status = SupplierPaymentStatus.COMPLETED;
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
    @Column(length = 255)
    private String note;
}
