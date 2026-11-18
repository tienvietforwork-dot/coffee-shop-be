package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Tỉnh / thành phố quán giao tới, với phí ship mặc định cho mọi phường / xã trong tỉnh. Phường nào cần phí khác
 * hoặc không giao thì khai báo ở {@link DeliveryWard}. Tỉnh không có trong bảng (hoặc đang tắt) = không giao.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_provinces")
public class DeliveryProvince {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** tên theo danh mục 34 tỉnh / thành (VD "Thành phố Hà Nội") */
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;
    /** thời gian giao mặc định (phút) tới mọi phường / xã trong tỉnh */
    @Builder.Default
    @Column(name = "delivery_minutes", nullable = false)
    private Integer deliveryMinutes = 30;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    @Builder.Default
    @OrderBy("name ASC")
    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryWard> wards = new ArrayList<>();
}
