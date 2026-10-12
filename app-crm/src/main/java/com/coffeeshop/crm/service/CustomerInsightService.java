package com.coffeeshop.crm.service;

import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerInsightRepository;
import com.coffeeshop.crm.repository.CustomerAnalyticsRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerInsightService {
    private final CustomerInsightRepository insightRepository;
    private final CustomerAnalyticsRepository analyticsRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.CustomerMetricRow> topCustomers(LocalDate from, LocalDate to, int limit) {
        return topCustomers(from, to, limit, "spend");
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.CustomerMetricRow> topCustomers(LocalDate from, LocalDate to, int limit, String sortBy) {
        validatePeriod(from, to);
        validateLimit(limit);
        if (!List.of("spend", "orders", "average").contains(sortBy)) {
            throw new IllegalArgumentException("sortBy chỉ nhận spend, orders hoặc average");
        }
        return insightRepository.topCustomers(start(from), end(to), limit, sortBy);
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.CustomerMetricRow> inactiveCustomers(int inactiveDays, int limit) {
        if (inactiveDays < 1 || inactiveDays > 3650) throw new IllegalArgumentException("inactiveDays phải từ 1 đến 3650");
        validateLimit(limit);
        return insightRepository.inactiveCustomers(inactiveDays, limit);
    }

    @Transactional(readOnly = true)
    public CustomerInsightRepository.GroupMetricRow groupMetrics(Long groupId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        CustomerInsightRepository.GroupMetricRow result = insightRepository.groupMetrics(groupId, start(from), end(to));
        if (result == null) throw new CrmNotFoundException("Không tìm thấy nhóm khách hàng ID " + groupId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.MonthlyMetricRow> monthlySales(LocalDate from, LocalDate to) {
        return monthlySales(from, to, "month");
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.MonthlyMetricRow> monthlySales(LocalDate from, LocalDate to, String period) {
        validatePeriod(from, to);
        if (!List.of("week", "month", "quarter").contains(period)) {
            throw new IllegalArgumentException("period chỉ nhận week, month hoặc quarter");
        }
        return insightRepository.salesTrends(start(from), end(to), period);
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.FeedbackMetricRow> feedbackSummary(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return insightRepository.feedbackMetrics(start(from), end(to));
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.MonthlyGrowthRow> customerGrowth(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return insightRepository.monthlyCustomerGrowth(start(from), end(to));
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.CustomerValueRow> customerValue(int limit) {
        validateLimit(limit);
        return insightRepository.customerValue(limit);
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.ProductAffinityRow> productAffinity(String dimension, LocalDate from, LocalDate to, int limit) {
        validatePeriod(from, to);
        validateLimit(limit);
        if (!List.of("coffee", "category").contains(dimension)) {
            throw new IllegalArgumentException("dimension chỉ nhận coffee hoặc category");
        }
        return insightRepository.productAffinity(dimension, start(from), end(to), limit);
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightRepository.RepeatedFeedbackRow> repeatedComplaints(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return insightRepository.repeatedComplaints(start(from), end(to));
    }

    @Transactional(readOnly = true)
    public PeriodComparison compareCustomerPeriods(Long customerId, LocalDate firstFrom, LocalDate firstTo,
                                                    LocalDate secondFrom, LocalDate secondTo) {
        validatePeriod(firstFrom, firstTo);
        validatePeriod(secondFrom, secondTo);
        if (!customerRepository.existsById(customerId)) throw new CrmNotFoundException("Không tìm thấy khách hàng ID " + customerId);
        var first = analyticsRepository.metrics(customerId, start(firstFrom), end(firstTo));
        var second = analyticsRepository.metrics(customerId, start(secondFrom), end(secondTo));
        return new PeriodComparison(customerId,
                new PeriodMetrics(first.totalSpend(), first.totalOrders(), first.averageOrderValue(), first.lastOrderAt()),
                new PeriodMetrics(second.totalSpend(), second.totalOrders(), second.averageOrderValue(), second.lastOrderAt()));
    }

    private static LocalDateTime start(LocalDate value) { return value == null ? null : value.atStartOfDay(); }
    private static LocalDateTime end(LocalDate value) { return value == null ? null : value.plusDays(1).atStartOfDay(); }
    private static void validateLimit(int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit phải từ 1 đến 500");
    }
    private static void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("fromDate phải nhỏ hơn hoặc bằng toDate");
    }

    public record PeriodMetrics(java.math.BigDecimal totalSpend, long totalOrders,
                                java.math.BigDecimal averageOrderValue, LocalDateTime lastOrderAt) { }
    public record PeriodComparison(Long customerId, PeriodMetrics firstPeriod, PeriodMetrics secondPeriod) { }
}
