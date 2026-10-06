package com.coffeeshop.repository;

import com.coffeeshop.entity.Promotion;
import com.coffeeshop.entity.PromotionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @Query("""
            select p from Promotion p
            where p.status = com.coffeeshop.entity.enums.PromotionStatus.ACTIVE
              and p.applyScope = com.coffeeshop.entity.enums.PromotionScope.ORDER
              and p.startDate <= :now and p.endDate >= :now
            """)
    List<Promotion> findRunningOrderPromotions(LocalDateTime now);

    @Query("""
            select i from PromotionItem i join fetch i.promotion p
            where p.status = com.coffeeshop.entity.enums.PromotionStatus.ACTIVE
              and p.applyScope = com.coffeeshop.entity.enums.PromotionScope.COFFEE
              and p.startDate <= :now and p.endDate >= :now
            """)
    List<PromotionItem> findRunningCoffeeItems(LocalDateTime now);
}
