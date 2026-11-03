package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ca thu ngân (mở ca / chốt ca, bàn giao ca). One register: one open shift at a time.
 * Tiền mặt lý thuyết = tiền đầu ca + thu tiền mặt bán hàng + thu khác − chi trong ca; chênh lệch = thực đếm − lý thuyết.
 * The figures are frozen when the shift is closed; a closed shift is never edited or reopened.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cash_shifts")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE cash_shifts SET del_flag = true WHERE id = ?")
public class CashShift extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    /** thu ngân mở ca */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;
    /** tiền đầu ca (tiền lẻ để thối) */
    @Column(name = "opening_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingCash;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private Staff closedBy;
    // ---- frozen at closing ----
    @Column(name = "order_count")
    private Integer orderCount;
    @Column(name = "cash_sales", precision = 12, scale = 2)
    private BigDecimal cashSales;
    @Column(name = "transfer_sales", precision = 12, scale = 2)
    private BigDecimal transferSales;
    @Column(name = "card_sales", precision = 12, scale = 2)
    private BigDecimal cardSales;
    @Column(name = "ewallet_sales", precision = 12, scale = 2)
    private BigDecimal ewalletSales;
    @Column(name = "cash_in", precision = 12, scale = 2)
    private BigDecimal cashIn;
    @Column(name = "cash_out", precision = 12, scale = 2)
    private BigDecimal cashOut;
    /** tiền mặt lý thuyết */
    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;
    /** tiền mặt thực đếm (Σ mệnh giá) */
    @Column(name = "counted_cash", precision = 12, scale = 2)
    private BigDecimal countedCash;
    /** đếm theo mệnh giá: "500000:2,200000:5,…" */
    @Column(length = 255)
    private String denominations;
    @Column(name = "difference_reason", length = 255)
    private String differenceReason;
    /** để lại cho ca sau */
    @Column(name = "next_opening_cash", precision = 12, scale = 2)
    private BigDecimal nextOpeningCash;
    /** nộp lại quản lý / chủ quán */
    @Column(name = "handover_cash", precision = 12, scale = 2)
    private BigDecimal handoverCash;
    /** người nhận bàn giao */
    @Column(name = "received_by", length = 100)
    private String receivedBy;
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<CashMovement> movements = new ArrayList<>();

    public boolean isOpen() {
        return closedAt == null;
    }
}
