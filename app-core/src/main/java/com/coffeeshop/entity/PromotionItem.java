package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotion_items")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE promotion_items SET del_flag = true WHERE id = ?")
public class PromotionItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_id", nullable = false)
    private Coffee coffee;
    @Column(name = "custom_discount", precision = 12, scale = 2)
    private BigDecimal customDiscount;
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;
    @Column(name = "max_quantity")
    private Integer maxQuantity;
    @Builder.Default
    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    public boolean hasRemaining(int qty) {
        return maxQuantity == null || soldQuantity + qty <= maxQuantity;
    }
}
