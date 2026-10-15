package com.coffeeshop.stats.entity;

import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "coffees")
@SQLRestriction("del_flag = false")
public class Coffee extends AuditableEntity {

    private static final String IMAGE_URL_PREFIX = "/api/public/images/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoffeeStatus status = CoffeeStatus.AVAILABLE;

    @Column(name = "auto_sold_out", nullable = false)
    private boolean autoSoldOut = false;

    public String imageSrc() {
        return imageId != null ? IMAGE_URL_PREFIX + imageId : imageUrl;
    }
}
