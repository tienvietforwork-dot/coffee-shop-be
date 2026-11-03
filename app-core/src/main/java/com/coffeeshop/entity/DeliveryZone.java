package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Khu vực giao hàng: the quận / phường it covers, its phí ship, đơn tối thiểu để giao, miễn phí ship từ … đ and the
 * usual delivery time. A district in no active zone is outside the delivery area.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_zones")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE delivery_zones SET del_flag = true WHERE id = ?")
public class DeliveryZone extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    /** quận / phường covered, comma separated: "Quận 1, Quận 3, Phú Nhuận" */
    @Column(nullable = false, length = 500)
    private String districts;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;
    /** miễn phí ship khi tiền hàng (sau giảm giá) từ mức này; null = không miễn phí */
    @Column(name = "free_ship_threshold", precision = 12, scale = 2)
    private BigDecimal freeShipThreshold;
    /** đơn tối thiểu để giao tới khu vực này; null = không giới hạn */
    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public List<String> districtList() {
        return Arrays.stream(districts.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
