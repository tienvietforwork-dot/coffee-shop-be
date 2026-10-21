package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.SupplierStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "suppliers")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE suppliers SET del_flag = true WHERE id = ?")
public class Supplier extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "contact_name", length = 100)
    private String contactName;
    @Column(length = 15)
    private String phone;
    @Column(length = 100)
    private String email;
    @Column(length = 255)
    private String address;
    @Column(name = "tax_code", length = 20)
    private String taxCode;
    @Column(length = 255)
    private String note;
    /** điều khoản thanh toán: số ngày được nợ kể từ ngày nhập hàng (0 = trả ngay khi nhận hàng) */
    @Builder.Default
    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays = 0;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierStatus status = SupplierStatus.ACTIVE;
    /** materials this supplier sells, with the usual packaging and a reference price */
    @Builder.Default
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    @SQLRestriction("del_flag = false")
    @OrderBy("id")
    private List<SupplierMaterial> materials = new ArrayList<>();
}
