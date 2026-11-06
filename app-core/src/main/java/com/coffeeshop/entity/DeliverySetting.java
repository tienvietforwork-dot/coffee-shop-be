package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Cấu hình giao hàng (1 dòng, id = 1): quán tự giao, phí ship tính theo quãng đường từ quán tới địa chỉ khách
 * (bậc phí ở {@link DeliveryFeeTier}); miễn phí ship từ … đ, đơn tối thiểu để giao.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_settings")
public class DeliverySetting extends BaseEntity {
    public static final long ID = 1L;

    @Id
    private Long id;
    /** điểm xuất phát để đo km */
    @Column(name = "shop_address", nullable = false, length = 255)
    private String shopAddress;
    @Column(name = "shop_lat")
    private Double shopLat;
    @Column(name = "shop_lng")
    private Double shopLng;
    /** giờ mở cửa (giờ Việt Nam); null = mở cả ngày. closeTime &lt; openTime = đóng sau nửa đêm */
    @Convert(converter = HhmmConverter.class)
    @Column(name = "open_time", length = 5)
    private java.time.LocalTime openTime;
    @Convert(converter = HhmmConverter.class)
    @Column(name = "close_time", length = 5)
    private java.time.LocalTime closeTime;
    /** miễn phí ship khi tiền hàng (sau giảm giá) từ mức này; null = không miễn phí */
    @Column(name = "free_ship_threshold", precision = 12, scale = 2)
    private BigDecimal freeShipThreshold;
    /** đơn tối thiểu để giao; null = không giới hạn */
    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;
    /** thời gian di chuyển (phút / km) để ước tính giờ giao tới; null = không ước tính */
    @Column(name = "minutes_per_km", precision = 4, scale = 1)
    private BigDecimal minutesPerKm;

    /**
     * Thời tiết xấu (tại quán, giờ giao): phụ phí theo bậc lượng mưa ({@link DeliveryWeatherTier}) cộng vào phí ship mọi
     * khung giờ; mưa từ {@code weatherStopRainMm} mm/h hoặc có giông thì ngưng nhận giao.
     */
    @Builder.Default
    @Column(name = "weather_enabled", nullable = false)
    private boolean weatherEnabled = false;
    /** ngưng giao khi mưa từ … mm / giờ; null = không ngưng theo lượng mưa */
    @Column(name = "weather_stop_rain_mm", precision = 5, scale = 1)
    private BigDecimal weatherStopRainMm;
    /** ghi chú cho khách khi có phụ phí thời tiết; null = câu mặc định */
    @Column(name = "weather_note", length = 255)
    private String weatherNote;
}
