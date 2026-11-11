package com.coffeeshop.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import com.coffeeshop.stats.util.ShopTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MerchandisingAnalyticsService {
    private final NamedParameterJdbcTemplate jdbc;

    private MapSqlParameterSource period(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.isBefore(LocalDate.of(2000,1,1))
                || to.isAfter(ShopTime.today().plusDays(1)) || from.plusYears(3).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ (tối đa 3 năm)");
        }
        return new MapSqlParameterSource().addValue("from", from.atStartOfDay())
                .addValue("to", to.plusDays(1).atStartOfDay());
    }
    private List<Map<String,Object>> query(String sql, MapSqlParameterSource params) {
        return jdbc.queryForList(sql, params);
    }
    public Map<String,Object> report(LocalDate from, LocalDate to) {
        var p=period(from,to);
        // Sales are actual completed orders, not attributable to a placement impression.
        var products=query("""
            SELECT c.id AS coffee_id, c.name AS coffee_name,
                   COALESCE(SUM(oi.quantity),0)::bigint AS units_sold,
                   COALESCE(SUM(oi.line_total),0) AS line_revenue,
                   COUNT(DISTINCT o.id)::bigint AS completed_orders
            FROM coffees c
            LEFT JOIN order_items oi ON oi.coffee_id=c.id AND oi.del_flag=false
            LEFT JOIN orders o ON o.id=oi.order_id AND o.status='COMPLETED'
                AND o.del_flag=false AND o.ordered_at>=:from AND o.ordered_at<:to
            WHERE c.del_flag=false
            GROUP BY c.id,c.name ORDER BY units_sold DESC, c.id LIMIT 300
            """.replace("SUM(oi.quantity)","SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END)")
                .replace("SUM(oi.line_total)","SUM(CASE WHEN o.id IS NOT NULL THEN oi.line_total ELSE 0 END)"),p);
        var bundles=query("""
            SELECT b.id AS bundle_id, b.name AS bundle_name,
                   COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN s.quantity ELSE 0 END),0)::bigint AS units_sold,
                   COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN s.quantity*s.unit_price+s.optional_total ELSE 0 END),0) AS revenue
            FROM product_bundles b LEFT JOIN order_bundle_sales s ON s.bundle_id=b.id
            LEFT JOIN orders o ON o.id=s.order_id AND o.status='COMPLETED' AND o.del_flag=false
              AND o.ordered_at>=:from AND o.ordered_at<:to
            GROUP BY b.id,b.name ORDER BY units_sold DESC, b.id LIMIT 200
            """,p);
        var cross=query("""
            SELECT r.id AS rule_id, r.name AS rule_name,
              COUNT(e.id) FILTER (WHERE e.event_type='IMPRESSION')::bigint AS impressions,
              COUNT(e.id) FILTER (WHERE e.event_type='CLICK')::bigint AS clicks
            FROM cross_sell_rules r LEFT JOIN cross_sell_events e ON e.rule_id=r.id
              AND e.created_at>=:from AND e.created_at<:to
            GROUP BY r.id,r.name ORDER BY impressions DESC,r.id
            """,p);
        var upsell=query("""
            SELECT r.id AS rule_id, r.name AS rule_name,
              COUNT(e.id) FILTER (WHERE e.event_type='IMPRESSION')::bigint AS impressions,
              COUNT(e.id) FILTER (WHERE e.event_type='ACCEPT')::bigint AS accepts
            FROM upsell_rules r LEFT JOIN upsell_events e ON e.rule_id=r.id
              AND e.created_at>=:from AND e.created_at<:to
            GROUP BY r.id,r.name ORDER BY impressions DESC,r.id
            """,p);
        var recommendations=query("""
            SELECT r.id AS rule_id, r.name AS rule_name,
              COUNT(e.id) FILTER (WHERE e.event_type='IMPRESSION')::bigint AS impressions,
              COUNT(e.id) FILTER (WHERE e.event_type='CLICK')::bigint AS clicks,
              COUNT(e.id) FILTER (WHERE e.event_type='ADD_TO_CART')::bigint AS add_to_cart
            FROM recommendation_rules r LEFT JOIN recommendation_events e ON e.rule_id=r.id
              AND e.created_at>=:from AND e.created_at<:to
            GROUP BY r.id,r.name ORDER BY impressions DESC,r.id
            """,p);
        var orders=query("""
            SELECT COUNT(*)::bigint AS orders, COALESCE(SUM(total_amount),0) AS revenue,
              COALESCE(AVG(total_amount),0) AS aov
            FROM orders WHERE status='COMPLETED' AND del_flag=false
              AND ordered_at>=:from AND ordered_at<:to
            """,p);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("from",from.toString()); result.put("to",to.toString());
        result.put("orders",orders.get(0)); result.put("products",products); result.put("bundles",bundles);
        result.put("crossSell",cross); result.put("upsell",upsell); result.put("recommendations",recommendations);
        result.put("tracking",Map.of(
                "productImpressionsAvailable",false,"productClicksAvailable",false,
                "orderAttributionAvailable",false,"averageOrderValueComparisonAvailable",false));
        result.put("note","Doanh thu món, combo và AOV từ đơn COMPLETED; lượt tương tác từ event. Chưa theo dõi product impressions/clicks/add-to-cart đồng nhất và chưa nối sự kiện vào đơn hàng để xác định conversion hoặc uplift theo chiến lược.");
        return result;
    }
}
