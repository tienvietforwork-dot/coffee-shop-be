package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.RecommendationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RecommendationRuleRepository extends JpaRepository<RecommendationRule,Long> {
    List<RecommendationRule> findAllByOrderByPriorityDescIdDesc();
    List<RecommendationRule> findByStatusOrderByPriorityDescIdDesc(String status);
}
