package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.UpsellRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public interface UpsellRuleRepository extends JpaRepository<UpsellRule,Long> {
    @EntityGraph(attributePaths={"anchorCoffee","upgradeCoffee"})
    List<UpsellRule> findAllByOrderByPriorityDescIdDesc();
    @EntityGraph(attributePaths={"anchorCoffee","upgradeCoffee"})
    @Query("select r from UpsellRule r where r.status='ACTIVE' and r.anchorCoffee.id=:anchorId and r.minCartAmount <= :total and (r.startsAt is null or r.startsAt <= :now) and (r.endsAt is null or r.endsAt >= :now) order by r.priority desc,r.id desc")
    List<UpsellRule> eligible(@Param("anchorId") long anchorId,@Param("total") BigDecimal total,@Param("now") LocalDateTime now);
}
