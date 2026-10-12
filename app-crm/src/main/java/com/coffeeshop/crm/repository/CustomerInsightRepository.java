package com.coffeeshop.crm.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerInsightRepository {
    @PersistenceContext private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<CustomerMetricRow> topCustomers(LocalDateTime from, LocalDateTime to, int limit) {
        return topCustomers(from, to, limit, "spend");
    }

    @SuppressWarnings("unchecked")
    public List<CustomerMetricRow> topCustomers(LocalDateTime from, LocalDateTime to, int limit, String sortBy) {
        String sortExpression = switch (sortBy) {
            case "orders" -> "count(o.id) desc, coalesce(sum(o.total_amount), 0) desc";
            case "average" -> "coalesce(avg(o.total_amount), 0) desc, count(o.id) desc";
            default -> "coalesce(sum(o.total_amount), 0) desc, count(o.id) desc";
        };
        String sql = """
                select c.id, c.full_name, c.phone, coalesce(sum(o.total_amount), 0), count(o.id),
                       coalesce(avg(o.total_amount), 0), max(o.ordered_at),
                       current_date - max(o.ordered_at)::date
                from customers c
                left join orders o on o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                where c.del_flag = false
                group by c.id, c.full_name, c.phone
                order by __SORT__, c.id
                limit :rowLimit
                """.replace("__SORT__", sortExpression);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        query.setParameter("rowLimit", limit);
        return mapMetrics(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<CustomerMetricRow> inactiveCustomers(int inactiveDays, int limit) {
        Query query = entityManager.createNativeQuery("""
                select c.id, c.full_name, c.phone, coalesce(sum(o.total_amount), 0), count(o.id),
                       coalesce(avg(o.total_amount), 0), max(o.ordered_at),
                       current_date - max(o.ordered_at)::date
                from customers c
                left join orders o on o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED'
                where c.del_flag = false
                group by c.id, c.full_name, c.phone
                having max(o.ordered_at) is null or max(o.ordered_at) < :cutoff
                order by max(o.ordered_at) asc nulls first
                limit :rowLimit
                """);
        query.setParameter("cutoff", LocalDateTime.now().minusDays(inactiveDays));
        query.setParameter("rowLimit", limit);
        return mapMetrics(query.getResultList());
    }

    public GroupMetricRow groupMetrics(Long groupId, LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                with members as (
                    select customer_id from customer_group_members
                    where group_id = :groupId and del_flag = false and is_excluded = false
                )
                select g.id, g.name, count(distinct m.customer_id), coalesce(sum(o.total_amount), 0),
                       count(distinct o.id), coalesce(avg(o.total_amount), 0),
                       (select cat.name from members fm join orders fo on fo.customer_id = fm.customer_id
                        join order_items foi on foi.order_id = fo.id and foi.del_flag = false
                        join coffees fc on fc.id = foi.coffee_id and fc.del_flag = false
                        join categories cat on cat.id = fc.category_id and cat.del_flag = false
                        where fo.del_flag = false and fo.status = 'COMPLETED'
                          and (cast(:fromDate as timestamp) is null or fo.ordered_at >= :fromDate)
                          and (cast(:toDate as timestamp) is null or fo.ordered_at < :toDate)
                        group by cat.id, cat.name order by sum(foi.quantity) desc, cat.name limit 1),
                       (select fc.name from members fm join orders fo on fo.customer_id = fm.customer_id
                        join order_items foi on foi.order_id = fo.id and foi.del_flag = false
                        join coffees fc on fc.id = foi.coffee_id and fc.del_flag = false
                        where fo.del_flag = false and fo.status = 'COMPLETED'
                          and (cast(:fromDate as timestamp) is null or fo.ordered_at >= :fromDate)
                          and (cast(:toDate as timestamp) is null or fo.ordered_at < :toDate)
                        group by fc.id, fc.name order by sum(foi.quantity) desc, fc.name limit 1),
                       (select count(*) from interactions i join members im on im.customer_id = i.customer_id
                        where i.del_flag = false and (cast(:fromDate as timestamp) is null or i.occurred_at >= :fromDate)
                          and (cast(:toDate as timestamp) is null or i.occurred_at < :toDate)),
                       (select count(*) from feedbacks f join members fmem on fmem.customer_id = f.customer_id
                        where f.del_flag = false and (cast(:fromDate as timestamp) is null or f.created_at >= :fromDate)
                          and (cast(:toDate as timestamp) is null or f.created_at < :toDate))
                from customer_groups g
                left join customer_group_members m on m.group_id = g.id and m.del_flag = false and m.is_excluded = false
                left join orders o on o.customer_id = m.customer_id and o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                where g.id = :groupId and g.del_flag = false
                group by g.id, g.name
                """);
        query.setParameter("groupId", groupId);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) return null;
        Object[] row = (Object[]) rows.get(0);
        return new GroupMetricRow(((Number) row[0]).longValue(), (String) row[1], number(row[2]),
                decimal(row[3]), number(row[4]), decimal(row[5]), (String) row[6], (String) row[7], number(row[8]), number(row[9]));
    }

    @SuppressWarnings("unchecked")
    public List<CustomerValueRow> customerValue(int limit) {
        Query query = entityManager.createNativeQuery("""
                select c.id, c.full_name, c.phone, c.loyalty_points, c.registered_at,
                       coalesce(sum(o.total_amount), 0), count(o.id), coalesce(avg(o.total_amount), 0),
                       max(o.ordered_at), current_date - max(o.ordered_at)::date,
                       current_date - c.registered_at::date
                from customers c
                left join orders o on o.customer_id = c.id and o.del_flag = false and o.status = 'COMPLETED'
                where c.del_flag = false
                group by c.id, c.full_name, c.phone, c.loyalty_points, c.registered_at
                order by coalesce(sum(o.total_amount), 0) desc, count(o.id) desc, c.id
                limit :rowLimit
                """);
        query.setParameter("rowLimit", limit);
        List<CustomerValueRow> result = new ArrayList<>();
        for (Object item : query.getResultList()) {
            Object[] row = (Object[]) item;
            result.add(new CustomerValueRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                    number(row[3]), dateTime(row[4]), decimal(row[5]), number(row[6]), decimal(row[7]),
                    dateTime(row[8]), row[9] == null ? null : ((Number) row[9]).intValue(),
                    row[10] == null ? null : ((Number) row[10]).intValue()));
        }
        return result;
    }

    public List<ProductAffinityRow> productAffinity(String dimension, LocalDateTime from, LocalDateTime to, int limit) {
        boolean byCategory = "category".equals(dimension);
        String id = byCategory ? "cat.id" : "co.id";
        String name = byCategory ? "cat.name" : "co.name";
        String joins = byCategory
                ? "join categories cat on cat.id = co.category_id and cat.del_flag = false"
                : "";
        Query query = entityManager.createNativeQuery("""
                select __ID__, __NAME__, count(distinct o.customer_id), count(distinct o.id),
                       coalesce(sum(oi.quantity), 0), coalesce(sum(oi.line_total), 0)
                from orders o
                join order_items oi on oi.order_id = o.id and oi.del_flag = false
                join coffees co on co.id = oi.coffee_id and co.del_flag = false
                __JOINS__
                where o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                group by __ID__, __NAME__
                order by sum(oi.quantity) desc, sum(oi.line_total) desc, __NAME__
                limit :rowLimit
                """.replace("__ID__", id).replace("__NAME__", name).replace("__JOINS__", joins));
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        query.setParameter("rowLimit", limit);
        List<ProductAffinityRow> result = new ArrayList<>();
        for (Object item : query.getResultList()) {
            Object[] row = (Object[]) item;
            result.add(new ProductAffinityRow(((Number) row[0]).longValue(), (String) row[1], number(row[2]),
                    number(row[3]), number(row[4]), decimal(row[5])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<MonthlyMetricRow> monthlySales(LocalDateTime from, LocalDateTime to) {
        return salesTrends(from, to, "month");
    }

    public List<MonthlyMetricRow> salesTrends(LocalDateTime from, LocalDateTime to, String period) {
        String bucket = "date_trunc('" + period + "', o.ordered_at)";
        Query query = entityManager.createNativeQuery("""
                select __BUCKET__, count(o.id), count(distinct o.customer_id),
                       coalesce(sum(o.total_amount), 0)
                from orders o
                where o.del_flag = false and o.status = 'COMPLETED'
                  and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                group by __BUCKET__
                order by __BUCKET__
                """.replace("__BUCKET__", bucket));
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        List<MonthlyMetricRow> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new MonthlyMetricRow(dateTime(row[0]), number(row[1]), number(row[2]), decimal(row[3])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<FeedbackMetricRow> feedbackMetrics(LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(issue_type, 'UNCLASSIFIED'), coalesce(category, 'UNCLASSIFIED'),
                       coalesce(sentiment, 'UNCLASSIFIED'), status,
                       count(*), count(distinct customer_id)
                from feedbacks
                where del_flag = false and (cast(:fromDate as timestamp) is null or created_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or created_at < :toDate)
                group by issue_type, category, sentiment, status
                order by count(*) desc
                """);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        List<FeedbackMetricRow> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new FeedbackMetricRow((String) row[0], (String) row[1], (String) row[2],
                    (String) row[3], number(row[4]), number(row[5])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<MonthlyGrowthRow> monthlyCustomerGrowth(LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                with first_purchase as (
                    select customer_id, date_trunc('month', min(ordered_at)) as first_month
                    from orders
                    where del_flag = false and status = 'COMPLETED' and customer_id is not null
                    group by customer_id
                ), monthly_orders as (
                    select date_trunc('month', ordered_at) as month,
                           count(distinct customer_id) as active_customers
                    from orders
                    where del_flag = false and status = 'COMPLETED' and customer_id is not null
                      and (cast(:fromDate as timestamp) is null or ordered_at >= :fromDate)
                      and (cast(:toDate as timestamp) is null or ordered_at < :toDate)
                    group by date_trunc('month', ordered_at)
                ), monthly_registered as (
                    select date_trunc('month', registered_at) as month, count(*) as new_profiles
                    from customers
                    where del_flag = false and (cast(:fromDate as timestamp) is null or registered_at >= :fromDate)
                      and (cast(:toDate as timestamp) is null or registered_at < :toDate)
                    group by date_trunc('month', registered_at)
                ), monthly_buyers as (
                    select date_trunc('month', o.ordered_at) as month,
                           count(distinct o.customer_id) filter (where fp.first_month = date_trunc('month', o.ordered_at)) as first_time_buyers
                    from orders o join first_purchase fp on fp.customer_id = o.customer_id
                    where o.del_flag = false and o.status = 'COMPLETED'
                      and (cast(:fromDate as timestamp) is null or o.ordered_at >= :fromDate)
                      and (cast(:toDate as timestamp) is null or o.ordered_at < :toDate)
                    group by date_trunc('month', o.ordered_at)
                ), all_months as (
                    select month from monthly_orders union select month from monthly_registered
                )
                select m.month, coalesce(r.new_profiles, 0), coalesce(o.active_customers, 0),
                       coalesce(b.first_time_buyers, 0),
                       greatest(coalesce(o.active_customers, 0) - coalesce(b.first_time_buyers, 0), 0)
                from all_months m
                left join monthly_registered r on r.month = m.month
                left join monthly_orders o on o.month = m.month
                left join monthly_buyers b on b.month = m.month
                order by m.month
                """);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        List<MonthlyGrowthRow> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new MonthlyGrowthRow(dateTime(row[0]), number(row[1]), number(row[2]), number(row[3]), number(row[4])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<RepeatedFeedbackRow> repeatedComplaints(LocalDateTime from, LocalDateTime to) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(nullif(ft.topic, ''), nullif(f.topic, ''), f.issue_type, f.category),
                       count(distinct f.id), count(distinct f.customer_id)
                from feedbacks f
                left join feedback_topics ft on ft.feedback_id = f.id
                where f.del_flag = false and f.category = 'COMPLAINT'
                  and (cast(:fromDate as timestamp) is null or f.created_at >= :fromDate)
                  and (cast(:toDate as timestamp) is null or f.created_at < :toDate)
                group by coalesce(nullif(ft.topic, ''), nullif(f.topic, ''), f.issue_type, f.category)
                having count(distinct f.id) > 1
                order by count(distinct f.id) desc
                """);
        query.setParameter("fromDate", from);
        query.setParameter("toDate", to);
        List<?> rows = query.getResultList();
        List<RepeatedFeedbackRow> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new RepeatedFeedbackRow((String) row[0], number(row[1]), number(row[2])));
        }
        return result;
    }

    private List<CustomerMetricRow> mapMetrics(List<?> rows) {
        List<CustomerMetricRow> result = new ArrayList<>(rows.size());
        for (Object value : rows) {
            Object[] row = (Object[]) value;
            result.add(new CustomerMetricRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                    decimal(row[3]), number(row[4]), decimal(row[5]), dateTime(row[6]),
                    row[7] == null ? null : ((Number) row[7]).intValue()));
        }
        return result;
    }

    private static java.math.BigDecimal decimal(Object value) {
        return value == null ? java.math.BigDecimal.ZERO : (java.math.BigDecimal) value;
    }
    private static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    private static LocalDateTime dateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof LocalDateTime time) return time;
        return null;
    }

    public record CustomerMetricRow(Long customerId, String fullName, String phone,
                                    java.math.BigDecimal totalSpend, long totalOrders,
                                    java.math.BigDecimal averageOrderValue, LocalDateTime lastOrderAt,
                                    Integer daysSinceLastOrder) { }
    public record GroupMetricRow(Long groupId, String groupName, long memberCount,
                                 java.math.BigDecimal totalSpend, long totalOrders,
                                 java.math.BigDecimal averageOrderValue, String favoriteCategory,
                                 String favoriteCoffee, long interactionCount, long feedbackCount) { }
    public record CustomerValueRow(Long customerId, String fullName, String phone, long loyaltyPoints,
                                   LocalDateTime registeredAt, java.math.BigDecimal totalSpend, long totalOrders,
                                   java.math.BigDecimal averageOrderValue, LocalDateTime lastOrderAt,
                                   Integer daysSinceLastOrder, Integer customerTenureDays) { }
    public record ProductAffinityRow(Long itemId, String itemName, long customerCount, long orderCount,
                                     long quantitySold, java.math.BigDecimal revenue) { }
    public record MonthlyMetricRow(LocalDateTime month, long totalOrders, long activeCustomers,
                                   java.math.BigDecimal revenue) { }
    public record FeedbackMetricRow(String issueType, String category, String sentiment, String status,
                                    long feedbackCount, long customerCount) { }
    public record MonthlyGrowthRow(LocalDateTime month, long newProfiles, long activeCustomers,
                                   long firstTimeBuyers, long returningCustomers) { }
    public record RepeatedFeedbackRow(String topic, long complaintCount, long customerCount) { }
}
