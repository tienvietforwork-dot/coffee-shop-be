package com.coffeeshop.stats.entity.repository;
import com.coffeeshop.stats.entity.CrossSellItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CrossSellItemRepository extends JpaRepository<CrossSellItem,Long> {
    List<CrossSellItem> findByRuleIdOrderByPositionAscIdAsc(Long ruleId);
}
