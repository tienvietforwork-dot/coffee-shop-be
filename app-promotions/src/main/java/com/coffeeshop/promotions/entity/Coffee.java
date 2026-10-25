package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.CoffeeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coffees")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE coffees SET del_flag = true WHERE id = ?")
public class Coffee extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "coffee")
    Set<PromotionItem> promotionItems = new HashSet<>();

    @Column(name = "category_id", nullable = false)
    private Long category;

    @Column(name = "name" ,nullable = false, length = 150)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price; // gia goc

    @Builder.Default
    @Column(name = "sale", precision = 5, scale = 2)
    private BigDecimal sale = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false )
    private CoffeeStatus status = CoffeeStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "auto_sold_out", nullable = false)
    private boolean autoSoldOut = false;



    public void updatePricing(BigDecimal salePrice, BigDecimal salePercentage) {
        this.salePrice = salePrice;
        this.sale = salePercentage;
    }

    public void resetSalePrice(){
        this.salePrice = this.price;
        this.sale = BigDecimal.ZERO;
    }

}
