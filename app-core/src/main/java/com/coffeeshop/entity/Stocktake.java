package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.StocktakeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiếu kiểm kê (biên bản kiểm kê vật tư, mẫu 05-VT): the book quantity of each lot is frozen when it is added,
 * the counted quantity is typed in, and "cân bằng kho" moves the stock by the difference (thừa / thiếu).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stocktakes")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE stocktakes SET del_flag = true WHERE id = ?")
public class Stocktake extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StocktakeStatus status = StocktakeStatus.DRAFT;
    /** người kiểm (trưởng ban kiểm kê) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    /** những người cùng kiểm (ban kiểm kê), free text */
    @Column(length = 255)
    private String members;
    @Column(name = "counted_at", nullable = false)
    private LocalDateTime countedAt;
    /** người duyệt cân bằng kho */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balanced_by")
    private Staff balancedBy;
    @Column(name = "balanced_at")
    private LocalDateTime balancedAt;
    /** ý kiến giải quyết số chênh lệch */
    @Column(length = 255)
    private String resolution;
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<StocktakeItem> items = new ArrayList<>();

    public BigDecimal surplusValue() {
        return items.stream().map(StocktakeItem::differenceValue).filter(v -> v.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal shortageValue() {
        return items.stream().map(StocktakeItem::differenceValue).filter(v -> v.signum() < 0).map(BigDecimal::negate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
