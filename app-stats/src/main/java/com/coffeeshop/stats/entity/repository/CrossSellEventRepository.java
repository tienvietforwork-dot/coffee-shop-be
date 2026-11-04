package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.CrossSellEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
public interface CrossSellEventRepository extends JpaRepository<CrossSellEvent,Long> {
    long countByRuleIdAndEventType(Long ruleId,String eventType);
    boolean existsByImpressionIdAndCoffeeIdAndEventType(UUID impressionId,Long coffeeId,String eventType);

    @Modifying
    @Query(value = """
    INSERT INTO public.cross_sell_events
        (rule_id, coffee_id, impression_id, event_type, created_at)
    VALUES
        (:ruleId, :coffeeId, :impressionId, :eventType, CURRENT_TIMESTAMP)
    ON CONFLICT (impression_id, coffee_id, event_type)
    DO NOTHING
    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("ruleId") Long ruleId,
            @Param("coffeeId") Long coffeeId,
            @Param("impressionId") UUID impressionId,
            @Param("eventType") String eventType
    );
}
