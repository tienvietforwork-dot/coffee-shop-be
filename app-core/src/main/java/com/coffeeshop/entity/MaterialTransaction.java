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
@Table(name = "material_transactions")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE material_transactions SET del_flag = true WHERE id = ?")
public class MaterialTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private MaterialBatch batch;
    /** Employee who performed the movement (from the logged-in account). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;
    /** PRODUCTION_USE: the prepared-material batch this stock went into */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produced_batch_id")
    private MaterialBatch producedBatch;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialTransactionType type;
    /** Signed: positive = stock in, negative = stock out. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
    @Column(length = 255)
    private String note;
}
