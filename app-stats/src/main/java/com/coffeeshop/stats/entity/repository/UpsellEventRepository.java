package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.UpsellEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
public interface UpsellEventRepository extends JpaRepository<UpsellEvent,Long> {
    long countByRuleIdAndEventType(Long ruleId,String eventType);
    boolean existsByRuleIdAndImpressionIdAndEventType(Long ruleId,UUID impressionId,String eventType);

    @Modifying
    @Query(value = """
    INSERT INTO public.upsell_events
        (rule_id, impression_id, event_type, created_at)
    VALUES
        (:ruleId, :impressionId, :eventType, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT
        upsell_events_rule_id_impression_id_event_type_key
    DO NOTHING
    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("ruleId") Long ruleId,
            @Param("impressionId") UUID impressionId,
            @Param("eventType") String eventType
    );
}
