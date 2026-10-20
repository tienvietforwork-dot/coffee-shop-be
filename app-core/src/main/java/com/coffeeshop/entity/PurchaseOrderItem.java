package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchase_order_items")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE purchase_order_items SET del_flag = true WHERE id = ?")
public class PurchaseOrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    /** packaging when ordered, copied from material_packages: package_count × package_size */
    @Column(name = "package_name", length = 50)
    private String packageName;
    @Column(name = "package_size", precision = 12, scale = 2)
    private BigDecimal packageSize;
    @Column(name = "package_count", precision = 12, scale = 2)
    private BigDecimal packageCount;
    /** ordered, in the material's unit */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
    @Builder.Default
    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;
    /** per unit of the material */
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(length = 255)
    private String note;

    public BigDecimal remaining() {
        return quantity.subtract(receivedQuantity).max(BigDecimal.ZERO);
    }
}
