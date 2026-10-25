package com.coffeeshop.stats.service;

import com.coffeeshop.stats.dto.response.CoffeePlacementResponse;
import com.coffeeshop.stats.entity.Coffee;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.repository.CoffeeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Data-backed merchandising suggestions, not automatically published. */
@Service
@RequiredArgsConstructor
public class ProductPlacementInsightsService {
    private final EntityManager entityManager;
    private final CoffeeRepository coffeeRepository;

    public record Suggestion(ProductPlacementSection section, CoffeePlacementResponse coffee,
                             long completedUnitsLast30Days, String reason) {}

    /** Anonymous guests receive a general ranking; customers are ranked by their past completed orders.
     * Never accept an arbitrary customerId from the public request.
     */
    @Transactional(readOnly = true)
    public List<Suggestion> personalized(Long customerId, int limit) {
        List<Suggestion> popular = suggest(ProductPlacementSection.RECOMMENDED,20);
        if(customerId==null) return popular.stream().limit(Math.max(1,Math.min(limit,12))).toList();
        @SuppressWarnings("unchecked")
        List<Object[]> rows=entityManager.createNativeQuery("""
            SELECT oi.coffee_id, COUNT(*) AS frequency FROM order_items oi
              JOIN orders o ON o.id=oi.order_id
            WHERE o.customer_id=:cid AND o.status='COMPLETED' AND o.del_flag=false AND oi.del_flag=false
            GROUP BY oi.coffee_id ORDER BY frequency DESC
        """).setParameter("cid",customerId).getResultList();
        if(rows.isEmpty()) return popular.stream().limit(Math.max(1,Math.min(limit,12))).toList();
        Map<Long,Long> likes=new HashMap<>();
        for(Object[] r:rows) likes.put(((Number)r[0]).longValue(),((Number)r[1]).longValue());
        // Interest score: historical frequency of a customer's coffee + general popularity.
        return popular.stream().sorted(Comparator.comparingLong((Suggestion v)->
                        likes.getOrDefault(v.coffee().id(),0L)*10000+v.completedUnitsLast30Days()).reversed())
                .limit(Math.max(1,Math.min(limit,12)))
                .map(v->new Suggestion(v.section(),v.coffee(),v.completedUnitsLast30Days(),
                        likes.containsKey(v.coffee().id())?"Phù hợp với lịch sử mua hàng":"Được nhiều khách hàng lựa chọn"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Suggestion> suggest(ProductPlacementSection section, int limit) {
        if (section != ProductPlacementSection.BEST_SELLER && section != ProductPlacementSection.NEW_ARRIVAL
                && section != ProductPlacementSection.RECOMMENDED) {
            throw new IllegalArgumentException("Chỉ hỗ trợ BEST_SELLER, NEW_ARRIVAL và RECOMMENDED");
        }
        int capped = Math.max(1, Math.min(20, limit));
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        // Only completed orders qualify; no estimation of bundle orders is performed.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT oi.coffee_id, SUM(oi.quantity) AS units
                FROM order_items oi JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'COMPLETED' AND o.del_flag = false AND oi.del_flag = false
                  AND o.ordered_at >= :start
                GROUP BY oi.coffee_id
                """).setParameter("start", start).getResultList();
        Map<Long, Long> units = new HashMap<>();
        for (Object[] row : rows) {
            units.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        @SuppressWarnings("unchecked")
        List<Object[]> createdRows = entityManager.createNativeQuery("""
                SELECT id, created_at FROM coffees WHERE del_flag = false
                """).getResultList();
        Map<Long, LocalDateTime> created = new HashMap<>();
        for (Object[] row : createdRows) {
            Object value = row[1];
            if (value instanceof java.sql.Timestamp ts) created.put(((Number)row[0]).longValue(), ts.toLocalDateTime());
            else if (value instanceof LocalDateTime dt) created.put(((Number)row[0]).longValue(), dt);
        }
        List<Coffee> available = coffeeRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getStatus() == CoffeeStatus.AVAILABLE && !c.isAutoSoldOut())
                .collect(Collectors.toCollection(ArrayList::new));
        if (section == ProductPlacementSection.NEW_ARRIVAL) {
            available.removeIf(c -> !created.containsKey(c.getId()) || created.get(c.getId()).isBefore(start));
            available.sort(Comparator.comparing((Coffee c) -> created.get(c.getId())).reversed());
        } else if (section == ProductPlacementSection.BEST_SELLER) {
            available.removeIf(c -> units.getOrDefault(c.getId(), 0L) == 0);
            available.sort(Comparator.comparingLong((Coffee c) -> units.getOrDefault(c.getId(), 0L)).reversed()
                    .thenComparing(Coffee::getId));
        } else {
            // Popular items across categories; deterministic ranking, not personalized recommendations.
            available.sort(Comparator.comparingLong((Coffee c) -> units.getOrDefault(c.getId(), 0L)).reversed()
                    .thenComparing(Coffee::getId));
        }
        String reason = section == ProductPlacementSection.NEW_ARRIVAL ? "Tạo trong 30 ngày qua"
                : section == ProductPlacementSection.BEST_SELLER ? "Số món trong đơn hoàn tất 30 ngày"
                : "Gợi ý theo lượng bán toàn cửa hàng, không cá nhân hóa";
        return available.stream().limit(capped)
                .map(c -> new Suggestion(section, CoffeePlacementResponse.from(c), units.getOrDefault(c.getId(), 0L), reason))
                .toList();
    }
}
