package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Option giá giao hàng: tên ("Mặc định", "Cao điểm"…) và các khoảng km → phí. Option mặc định luôn có, áp dụng cho
 * mọi giờ không thuộc khung giờ nào; các option khác được gán cho khung giờ ({@link DeliveryTimeSlot}), dùng lại được.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_fee_plans")
public class DeliveryFeePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60)
    private String name;
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean defaultPlan = false;
    /** ghi chú cho khách khi option này được áp dụng (khung giờ cao điểm…); null = câu mặc định */
    @Column(length = 255)
    private String note;
    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fromKm ASC")
    private List<DeliveryFeeTier> tiers = new ArrayList<>();
}
