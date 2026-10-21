package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** Dòng phiếu trả hàng nhập: {@code quantity} (material's unit) of one line of the original phiếu nhập. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supplier_return_items")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE supplier_return_items SET del_flag = true WHERE id = ?")
public class SupplierReturnItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_return_id", nullable = false)
    private SupplierReturn supplierReturn;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_item_id", nullable = false)
    private GoodsReceiptItem goodsReceiptItem;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
    /** giá trả, mặc định = đơn giá nhập */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(length = 255)
    private String note;

    public BigDecimal amount() {
        return Money.vnd(unitPrice.multiply(quantity));
    }
}
