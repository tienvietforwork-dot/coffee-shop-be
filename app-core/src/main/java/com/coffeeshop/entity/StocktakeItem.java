package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.StockQuality;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** One lot on a phiếu kiểm kê: SL theo sổ (frozen when added) vs SL thực tế; quantities in the material's unit. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stocktake_items")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE stocktake_items SET del_flag = true WHERE id = ?")
public class StocktakeItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stocktake_id", nullable = false)
    private Stocktake stocktake;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private MaterialBatch batch;
    @Column(name = "book_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookQuantity;
    /** null while not counted yet */
    @Column(name = "counted_quantity", precision = 12, scale = 2)
    private BigDecimal countedQuantity;
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockQuality quality = StockQuality.GOOD;
    /** nguyên nhân chênh lệch */
    @Column(length = 255)
    private String reason;

    /** thừa (+) / thiếu (−); 0 while not counted */
    public BigDecimal difference() {
        return countedQuantity == null ? BigDecimal.ZERO : countedQuantity.subtract(bookQuantity);
    }

    public BigDecimal differenceValue() {
        return unitCost == null ? BigDecimal.ZERO : Money.vnd(difference().multiply(unitCost));
    }
}
