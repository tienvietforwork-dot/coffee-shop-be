package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** Thu / chi tiền mặt ngoài bán hàng trong ca (nạp thêm tiền lẻ, mua đá, trả tiền ship…): IN or OUT. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cash_movements")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE cash_movements SET del_flag = true WHERE id = ?")
public class CashMovement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashShift shift;
    /** IN = thu, OUT = chi */
    @Column(nullable = false, length = 3)
    private String type;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 255)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
}
