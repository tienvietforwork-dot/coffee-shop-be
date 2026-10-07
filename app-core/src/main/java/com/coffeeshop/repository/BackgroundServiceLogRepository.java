package com.coffeeshop.repository;

import com.coffeeshop.entity.BackgroundServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackgroundServiceLogRepository extends JpaRepository<BackgroundServiceLog, Long> {
}
