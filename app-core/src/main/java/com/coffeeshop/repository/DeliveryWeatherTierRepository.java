package com.coffeeshop.repository;

import com.coffeeshop.entity.DeliveryWeatherTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryWeatherTierRepository extends JpaRepository<DeliveryWeatherTier, Long> {
    List<DeliveryWeatherTier> findAllByOrderByMinRainMmAsc();
}
