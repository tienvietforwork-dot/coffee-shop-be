package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Phiếu đặt hàng nhà cung cấp; see {@link PurchaseOrderStatus} for the life cycle. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchase_orders")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE purchase_orders SET del_flag = true WHERE id = ?")
public class PurchaseOrder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    /** Employee who drew up the order (from the logged-in account). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
    @Column(name = "expected_date")
    private LocalDate expectedDate;
    /** sent to the supplier */
    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;
    /** received in full, closed or cancelled */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(length = 255)
    private String note;
    /** closed short of the ordered quantities, or cancelled: why */
    @Column(name = "close_reason", length = 255)
    private String closeReason;
    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
