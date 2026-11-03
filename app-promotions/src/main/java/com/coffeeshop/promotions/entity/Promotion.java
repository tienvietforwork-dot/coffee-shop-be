package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.PromotionScope;
import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import com.coffeeshop.promotions.entity.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotions")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE promotions SET del_flag = true WHERE id = ?")
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_scope", nullable = false)
    private PromotionScope applyScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private PromotionType discountType;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromotionItem> promotionItems = new ArrayList<>();

    public void addPromotionItem(PromotionItem item) {
        promotionItems.add(item);
        item.setPromotion(this);
    }


}