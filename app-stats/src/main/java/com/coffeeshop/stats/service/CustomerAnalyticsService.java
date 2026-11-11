
package com.coffeeshop.stats.service;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.coffeeshop.stats.util.ShopTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer Analytics - UC94 to UC98.
 *
 * Read-only analytics based on completed,
 * non-deleted orders in PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAnalyticsService {

    private final NamedParameterJdbcTemplate jdbc;

    // ==========================================
    // VALIDATE DATE RANGE
    // ==========================================

    private MapSqlParameterSource params(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null
                || to == null
                || from.isAfter(to)
                || from.isBefore(LocalDate.of(2000, 1, 1))
                || to.isAfter(ShopTime.today().plusDays(1))
                || from.plusYears(3).isBefore(to)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khoảng ngày không hợp lệ (tối đa 3 năm)"
            );
        }

        return new MapSqlParameterSource()
                .addValue("from", from.atStartOfDay())
                .addValue("to", to.plusDays(1).atStartOfDay());
    }

    private List<Map<String, Object>> query(
            String sql,
            MapSqlParameterSource params
    ) {
        return jdbc.queryForList(sql, params);
    }

    // ==========================================
    // UC94 - PHAN TICH XU HUONG MUA HANG
    // ==========================================

    public List<Map<String, Object>> trends(
            LocalDate from,
            LocalDate to
    ) {

        return query("""
                SELECT
                    to_char(
                        date_trunc('month', o.ordered_at),
                        'YYYY-MM'
                    ) AS month,

                    count(*)::bigint AS orders,

                    count(
                        DISTINCT o.customer_id
                    )::bigint AS buyers,

                    coalesce(
                        sum(o.total_amount),
                        0
                    ) AS revenue,

                    coalesce(
                        round(avg(o.total_amount), 2),
                        0
                    ) AS avg_order_value

                FROM public.orders o

                WHERE o.status = 'COMPLETED'
                  AND o.del_flag = false
                  AND o.customer_id IS NOT NULL
                  AND o.ordered_at >= :from
                  AND o.ordered_at < :to

                GROUP BY 1
                ORDER BY 1
                """, params(from, to));
    }

    // ==========================================
    // UC95 - PHAN TICH GIA TRI KHACH HANG
    // ==========================================

    public List<Map<String, Object>> spending(
            LocalDate from,
            LocalDate to
    ) {

        return query("""
                SELECT
                    c.id AS customer_id,

                    coalesce(
                        nullif(c.full_name, ''),
                        'Khách #' || c.id
                    ) AS customer_name,

                    c.phone,

                    count(o.id)::bigint AS orders,

                    coalesce(
                        sum(o.total_amount),
                        0
                    ) AS total_spent,

                    coalesce(
                        round(avg(o.total_amount), 2),
                        0
                    ) AS avg_order_value,

                    max(o.ordered_at) AS last_order_at

                FROM public.orders o

                JOIN public.customers c
                    ON c.id = o.customer_id

                WHERE o.status = 'COMPLETED'
                  AND o.del_flag = false
                  AND c.del_flag = false
                  AND o.ordered_at >= :from
                  AND o.ordered_at < :to

                GROUP BY
                    c.id,
                    c.full_name,
                    c.phone

                ORDER BY
                    total_spent DESC,
                    orders DESC,
                    c.id

                LIMIT 500
                """, params(from, to));
    }

    // ==========================================
    // UC96 - SO SANH NHOM KHACH HANG
    //
    // FIX:
    // Bo dieu kien m.is_excluded = false
    // vi cot nay khong ton tai trong DB.
    //
    // DISTINCT thanh vien de tranh dem trung
    // so khach trong cung mot nhom.
    // ==========================================

    public List<Map<String, Object>> groups(
            LocalDate from,
            LocalDate to
    ) {

        return query("""
                SELECT
                    g.id AS group_id,

                    g.name AS group_name,

                    count(
                        DISTINCT m.customer_id
                    )::bigint AS members,

                    count(o.id)::bigint AS orders,

                    count(
                        DISTINCT o.customer_id
                    )::bigint AS buying_customers,

                    coalesce(
                        sum(o.total_amount),
                        0
                    ) AS revenue,

                    coalesce(
                        round(avg(o.total_amount), 2),
                        0
                    ) AS avg_order_value

                FROM public.customer_groups g

                LEFT JOIN public.customer_group_members m
                    ON m.group_id = g.id
                   AND m.del_flag = false

                LEFT JOIN public.orders o
                    ON o.customer_id = m.customer_id
                   AND o.del_flag = false
                   AND o.status = 'COMPLETED'
                   AND o.ordered_at >= :from
                   AND o.ordered_at < :to

                WHERE g.del_flag = false

                GROUP BY
                    g.id,
                    g.name

                ORDER BY
                    revenue DESC,
                    g.id
                """, params(from, to));
    }

    // ==========================================
    // UC97 - BAO CAO TANG TRUONG KHACH HANG
    // ==========================================

    public List<Map<String, Object>> growth(
            LocalDate from,
            LocalDate to
    ) {

        return query("""
                SELECT
                    to_char(
                        date_trunc(
                            'month',
                            c.registered_at
                        ),
                        'YYYY-MM'
                    ) AS month,

                    count(*)::bigint AS new_customers,

                    count(*) FILTER (
                        WHERE EXISTS (
                            SELECT 1

                            FROM public.orders o

                            WHERE o.customer_id = c.id
                              AND o.del_flag = false
                              AND o.status = 'COMPLETED'
                              AND o.ordered_at >= c.registered_at
                              AND o.ordered_at < :to
                        )
                    )::bigint AS converted_customers

                FROM public.customers c

                WHERE c.del_flag = false
                  AND c.registered_at >= :from
                  AND c.registered_at < :to

                GROUP BY 1
                ORDER BY 1
                """, params(from, to));
    }

    // ==========================================
    // UC98 - TONG HOP BAO CAO
    // ==========================================

    public Map<String, Object> report(
            LocalDate from,
            LocalDate to
    ) {

        // Validate truoc khi chay cac truy van
        params(from, to);

        List<Map<String, Object>> trends =
                trends(from, to);

        List<Map<String, Object>> spenders =
                spending(from, to);

        List<Map<String, Object>> groups =
                groups(from, to);

        List<Map<String, Object>> growth =
                growth(from, to);

        BigDecimal revenue = trends.stream()
                .map(row -> (BigDecimal) row.get("revenue"))
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        long orders = trends.stream()
                .mapToLong(row ->
                        ((Number) row.get("orders"))
                                .longValue()
                )
                .sum();

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put("from", from.toString());
        report.put("to", to.toString());

        report.put("orders", orders);
        report.put("revenue", revenue);

        report.put(
                "uniqueSpendingCustomers",
                spenders.size()
        );

        report.put(
                "spendingLimitedToTop",
                500
        );

        report.put("trends", trends);
        report.put("spending", spenders);
        report.put("groups", groups);
        report.put("growth", growth);

        report.put(
                "note",
                "Chỉ tính đơn COMPLETED; "
                        + "nhóm khách có thể chồng lấn; "
                        + "danh sách chi tiêu giới hạn top 500."
        );

        return report;
    }
}
