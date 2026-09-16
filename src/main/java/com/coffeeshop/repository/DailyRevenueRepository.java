package com.coffeeshop.repository;

import com.coffeeshop.entity.DailyRevenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, Long> {
    Optional<DailyRevenue> findByRevenueDate(LocalDate date);
    List<DailyRevenue> findByRevenueDateBetweenOrderByRevenueDateAsc(LocalDate from, LocalDate to);
}
