package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.RecommendationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent,Long> {
    long countByRuleIdAndEventType(Long ruleId,String eventType);
    @Modifying
    @Query(value="INSERT INTO public.recommendation_events(rule_id,coffee_id,impression_id,event_type) VALUES (:ruleId,:coffeeId,:impressionId,:type) ON CONFLICT (rule_id,coffee_id,impression_id,event_type) DO NOTHING",nativeQuery=true)
    int insertIfAbsent(@Param("ruleId") Long ruleId,@Param("coffeeId") Long coffeeId,@Param("impressionId") UUID impressionId,@Param("type") String type);
}
