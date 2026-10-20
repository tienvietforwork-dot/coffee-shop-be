package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/** Phiếu nhập kho: one delivery received, against a purchase order or loose (purchaseOrder null). Each line is a batch. */
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "goodsReceipt")
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<MaterialBatch> batches = new ArrayList<>();
}
