package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.Promotion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @EntityGraph(attributePaths = {"promotionItems", "promotionItems.coffee"})
    List<Promotion> findAll();
}