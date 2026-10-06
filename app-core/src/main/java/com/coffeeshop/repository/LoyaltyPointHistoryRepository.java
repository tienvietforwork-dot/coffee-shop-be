package com.coffeeshop.repository;

import com.coffeeshop.entity.LoyaltyPointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyPointHistoryRepository extends JpaRepository<LoyaltyPointHistory, Long> {
    List<LoyaltyPointHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
