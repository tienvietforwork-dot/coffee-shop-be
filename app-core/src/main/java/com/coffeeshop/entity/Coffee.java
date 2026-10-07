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
@Table(name = "coffees")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE coffees SET del_flag = true WHERE id = ?")
public class Coffee extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false, length = 150)
    private String name;
    /** Uploaded photo (images.id); takes precedence over {@link #imageUrl}, which is only for external links. */
    @Column(name = "image_id")
    private Long imageId;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoffeeStatus status = CoffeeStatus.AVAILABLE;
    /** true when SOLD_OUT was set by stock (StockService.syncCoffeeStatus), so it may reopen by itself; manual SOLD_OUT stays. */
    @Builder.Default
    @Column(name = "auto_sold_out", nullable = false)
    private boolean autoSoldOut = false;

    /** Image source for clients: the uploaded photo when there is one, else the external link. */
    public String imageSrc() {
        return imageId != null ? Image.URL_PREFIX + imageId : imageUrl;
    }
}
