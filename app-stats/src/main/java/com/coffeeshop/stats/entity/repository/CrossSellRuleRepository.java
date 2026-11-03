package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.CrossSellRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public interface CrossSellRuleRepository extends JpaRepository<CrossSellRule,Long> {
    List<CrossSellRule> findAllByOrderByPriorityDescIdDesc();
    @Query("select distinct r from CrossSellRule r join r.items i where r.status='ACTIVE' and r.anchorCoffee.id=:coffeeId and (r.startsAt is null or r.startsAt<=:now) and (r.endsAt is null or r.endsAt>=:now) and r.minCartAmount<=:amount order by r.priority desc, r.id asc")
    List<CrossSellRule> findEligible(@Param("coffeeId") long coffeeId,@Param("amount") BigDecimal amount,@Param("now") LocalDateTime now);
}
