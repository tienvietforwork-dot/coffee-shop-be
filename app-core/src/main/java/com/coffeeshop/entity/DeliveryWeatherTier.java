package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Bậc phụ phí theo lượng mưa: mưa từ {@code minRainMm} mm / giờ → cộng {@code surcharge} vào phí ship (lấy bậc cao nhất đạt). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_weather_tiers")
public class DeliveryWeatherTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "min_rain_mm", nullable = false, precision = 5, scale = 1)
    private BigDecimal minRainMm;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal surcharge;
}
