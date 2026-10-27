package com.coffeeshop.stats.entity;

import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "product_placement_rules")
@SQLRestriction("del_flag = false")
public class ProductPlacementRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_id", nullable = false)
    private Coffee coffee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductPlacementSection section;

    @Column(length = 150)
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    public boolean isVisibleNow(LocalDateTime now) {
        return active
                && publicationStatus == PublicationStatus.PUBLISHED
                && (startsAt == null || !startsAt.isAfter(now))
                && (endsAt == null || now.isBefore(endsAt));
    }
}
