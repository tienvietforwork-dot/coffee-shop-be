package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.VoucherTemplateCustomerType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateOrderType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplatesStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "voucher_templates")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE voucher_templates SET del_flag = true WHERE id = ?")
public class VoucherTemplates extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false )
    private Voucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private VoucherTemplateCustomerType customerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_group_id")
    private CustomerGroup customerGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private VoucherTemplateOrderType orderType;

    @Column(name = "completed_order_count")
    private Long completedOrderCount;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "required_points", nullable = false)
    private Long requiredPoints;

    @Column(name = "max_quantity", nullable = false)
    private Long maxQuantity;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherTemplatesStatus status;
}
