package com.coffeeshop.crm.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Reads Core-owned order data from the shared PostgreSQL database. */
@Repository
public class CustomerAnalyticsRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Metrics metrics(Long customerId, LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(sum(o.total_amount), 0), count(o.id), coalesce(avg(o.total_amount), 0), max(o.ordered_at),
                       (select count(*) from orders recent where recent.customer_id = :customerId
                        and recent.del_flag = false and recent.status = 'COMPLETED'
                        and recent.ordered_at >= current_timestamp - interval '30 days')
                from orders o
                where o.customer_id = :customerId and o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        Object[] row = (Object[]) query.getSingleResult();
        return new Metrics(decimal(row[0]), number(row[1]), decimal(row[2]), dateTime(row[3]), number(row[4]));
    }

    @SuppressWarnings("unchecked")
    public List<OrderSummary> recentOrders(Long customerId, LocalDateTime from, LocalDateTime to, int limit) {
        Query query = entityManager.createNativeQuery("""
                select o.order_code, o.status, o.total_amount, o.ordered_at,
                       coalesce(string_agg(c.name || ' x' || oi.quantity, ', ' order by c.name), '')
                from orders o
                left join order_items oi on oi.order_id = o.id and oi.del_flag = false
                left join coffees c on c.id = oi.coffee_id and c.del_flag = false
                where o.customer_id = :customerId and o.del_flag = false
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                group by o.id, o.order_code, o.status, o.total_amount, o.ordered_at
                order by o.ordered_at desc
                limit :rowLimit
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        query.setParameter("rowLimit", limit);
        List<Object[]> rows = query.getResultList();
        List<OrderSummary> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new OrderSummary((String) row[0], (String) row[1], decimal(row[2]), dateTime(row[3]), (String) row[4]));
        }
        return result;
    }

    public String favoriteCoffee(Long customerId, LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                select c.name
                from orders o
                join order_items oi on oi.order_id = o.id and oi.del_flag = false
                join coffees c on c.id = oi.coffee_id and c.del_flag = false
                where o.customer_id = :customerId and o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                group by c.id, c.name
                order by sum(oi.quantity) desc, c.name asc
                limit 1
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        return rows.isEmpty() ? null : (String) rows.get(0);
    }

    public String favoriteCategory(Long customerId, LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                select cat.name
                from orders o
                join order_items oi on oi.order_id = o.id and oi.del_flag = false
                join coffees c on c.id = oi.coffee_id and c.del_flag = false
                join categories cat on cat.id = c.category_id and cat.del_flag = false
                where o.customer_id = :customerId and o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                group by cat.id, cat.name
                order by sum(oi.quantity) desc, cat.name asc
                limit 1
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        return rows.isEmpty() ? null : (String) rows.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<VoucherEvent> recentVouchers(Long customerId, LocalDateTime from, LocalDateTime to, int limit) {
        Query query = entityManager.createNativeQuery("""
                select code, status, issued_at
                from voucher_details
                where customer_id = :customerId and del_flag = false
                  and (cast(:fromDate as timestamp) is null or issued_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or issued_at < :toDate)
                order by issued_at desc
                limit :rowLimit
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        query.setParameter("rowLimit", limit);
        List<?> rows = query.getResultList();
        List<VoucherEvent> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new VoucherEvent((String) row[0], (String) row[1], dateTime(row[2])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<PromotionEvent> recentPromotionUses(Long customerId, LocalDateTime from, LocalDateTime to, int limit) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(p.name, 'Khuyến mãi #' || o.promotion_id), o.order_code, o.status, o.ordered_at
                from orders o
                left join promotions p on p.id = o.promotion_id
                where o.customer_id = :customerId and o.promotion_id is not null and o.del_flag = false
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                order by o.ordered_at desc
                limit :rowLimit
                """);
        query.setParameter("customerId", customerId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        query.setParameter("rowLimit", limit);
        List<PromotionEvent> result = new ArrayList<>();
        for (Object item : query.getResultList()) {
            Object[] row = (Object[]) item;
            result.add(new PromotionEvent((String) row[0], (String) row[1], (String) row[2], dateTime(row[3])));
        }
        return result;
    }

    public List<Long> matchingCustomerIds(BigDecimal minSpend, BigDecimal maxSpend, Integer minOrders,
                                           Integer lastOrderDays, Long favoriteCategoryId, Long favoriteCoffeeId) {
        StringBuilder sql = new StringBuilder("select c.id from customers c where c.del_flag = false");
        if (minSpend != null || maxSpend != null) {
            sql.append(" and (select coalesce(sum(o.total_amount), 0) from orders o where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED')");
            if (minSpend != null) sql.append(" >= :minSpend");
            if (maxSpend != null) sql.append(minSpend != null ? " and (select coalesce(sum(o.total_amount), 0) from orders o where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED') <= :maxSpend" : " <= :maxSpend");
        }
        if (minOrders != null) {
            sql.append(" and (select count(o.id) from orders o where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED') >= :minOrders");
        }
        if (lastOrderDays != null) {
            sql.append(" and exists (select 1 from orders o where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED' and o.ordered_at >= :sinceDate)");
        }
        if (favoriteCategoryId != null) {
            sql.append(" and (select co.category_id from orders o join order_items oi on oi.order_id = o.id and oi.del_flag = false join coffees co on co.id = oi.coffee_id and co.del_flag = false where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED' group by co.category_id order by sum(oi.quantity) desc, co.category_id asc limit 1) = :categoryId");
        }
        if (favoriteCoffeeId != null) {
            sql.append(" and (select co.id from orders o join order_items oi on oi.order_id = o.id and oi.del_flag = false join coffees co on co.id = oi.coffee_id and co.del_flag = false where o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED' group by co.id order by sum(oi.quantity) desc, co.id asc limit 1) = :coffeeId");
        }
        sql.append(" order by c.id");
        Query query = entityManager.createNativeQuery(sql.toString());
        if (minSpend != null) query.setParameter("minSpend", minSpend);
        if (maxSpend != null) query.setParameter("maxSpend", maxSpend);
        if (minOrders != null) query.setParameter("minOrders", minOrders);
        if (lastOrderDays != null) query.setParameter("sinceDate", LocalDateTime.now().minusDays(lastOrderDays));
        if (favoriteCategoryId != null) query.setParameter("categoryId", favoriteCategoryId);
        if (favoriteCoffeeId != null) query.setParameter("coffeeId", favoriteCoffeeId);
        List<?> rows = query.getResultList();
        return rows.stream().map(row -> ((Number) row).longValue()).toList();
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static LocalDateTime dateTime(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        return null;
    }

    public record Metrics(BigDecimal totalSpend, long totalOrders, BigDecimal averageOrderValue,
                          LocalDateTime lastOrderAt, long ordersLast30Days) { }

    public record OrderSummary(String orderCode, String status, BigDecimal totalAmount,
                               LocalDateTime orderedAt, String items) { }
    public record VoucherEvent(String code, String status, LocalDateTime issuedAt) { }
    public record PromotionEvent(String promotionName, String orderCode, String orderStatus, LocalDateTime orderedAt) { }
}
