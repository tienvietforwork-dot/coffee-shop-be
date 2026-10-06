package com.coffeeshop.repository;

import com.coffeeshop.entity.PromotionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionItemRepository extends JpaRepository<PromotionItem, Long> {
}
