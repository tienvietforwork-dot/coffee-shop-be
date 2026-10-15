package com.coffeeshop.stats.entity;

import com.coffeeshop.stats.entity.enums.ProductBundleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "product_bundles")
@SQLRestriction("del_flag = false")
public class ProductBundle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "bundle_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal bundlePrice;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductBundleStatus status = ProductBundleStatus.DRAFT;

    @OrderBy("displayOrder ASC, id ASC")
    @OneToMany(mappedBy = "bundle", cascade = CascadeType.ALL)
    private List<ProductBundleItem> items = new ArrayList<>();

    public boolean isAvailableNow(LocalDateTime now) {
        return status == ProductBundleStatus.ACTIVE
                && (startsAt == null || !startsAt.isAfter(now))
                && (endsAt == null || !endsAt.isBefore(now));
    }
}
