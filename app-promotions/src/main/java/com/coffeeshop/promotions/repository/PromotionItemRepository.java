package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.PromotionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionItemRepository extends JpaRepository<PromotionItem, Long> {
}