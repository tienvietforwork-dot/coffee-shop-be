package com.coffeeshop.promotions.entity;

import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.entity.enums.VoucherType;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vouchers")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE vouchers SET del_flag = true WHERE id = ?")
public class Voucher extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VoucherDetail> voucherDetails = new ArrayList<>();

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private VoucherType discountType;

    @Column(name = "discount_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "valid_days", nullable = false)
    private Integer validDays;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherTypeStatus status = VoucherTypeStatus.ACTIVE;

    public void addVoucherDetail(VoucherDetail item){
        voucherDetails.add(item);
        item.setVoucher(this);
    }

}
