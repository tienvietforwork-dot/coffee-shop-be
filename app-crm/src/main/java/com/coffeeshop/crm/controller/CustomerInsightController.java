package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.repository.CustomerInsightRepository;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.CustomerGrowthResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.CustomerMetricResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.CustomerValueResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.FeedbackSummaryResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.FeedbackTrendResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.GroupMetricResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.PeriodComparisonResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.PeriodMetricsResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.ProductAffinityResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.RepeatedComplaintResponse;
import com.coffeeshop.crm.dto.response.CustomerInsightResponses.SalesTrendResponse;
import com.coffeeshop.crm.service.CustomerInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Tag(name = "Báo cáo và phân tích khách hàng", description = "Thống kê hành vi mua, giá trị, nhóm, tăng trưởng và phản hồi khách hàng")
public class CustomerInsightController {
    private final CustomerInsightService insightService;

    @GetMapping("/top-customers")
    @Operation(summary = "Xếp hạng khách theo chi tiêu, số đơn hoặc giá trị đơn trung bình")
    public ResponseEntity<List<CustomerMetricResponse>> topCustomers(@RequestParam(required = false) LocalDate fromDate,
                                          @RequestParam(required = false) LocalDate toDate,
                                          @RequestParam(defaultValue = "50") int limit,
                                          @RequestParam(defaultValue = "spend") String sortBy) {
        return ResponseEntity.ok(insightService.topCustomers(fromDate, toDate, limit, sortBy).stream()
                .map(CustomerInsightController::toCustomerMetric).toList());
    }

    @GetMapping("/inactive-customers")
    @Operation(summary = "Danh sách khách lâu chưa mua hoặc chưa từng mua")
    public ResponseEntity<List<CustomerMetricResponse>> inactiveCustomers(@RequestParam(defaultValue = "90") int inactiveDays,
                                               @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(insightService.inactiveCustomers(inactiveDays, limit).stream()
                .map(CustomerInsightController::toCustomerMetric).toList());
    }

    @GetMapping("/inactive-customers/count")
    @Operation(summary = "Đếm tổng số khách lâu chưa mua hoặc chưa từng mua")
    public ResponseEntity<Long> countInactiveCustomers(@RequestParam(defaultValue = "90") int inactiveDays) {
        return ResponseEntity.ok(insightService.countInactiveCustomers(inactiveDays));
    }

    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Thống kê giá trị và mức tương tác của một nhóm")
    public ResponseEntity<GroupMetricResponse> groupMetrics(@PathVariable Long groupId,
                                          @RequestParam(required = false) LocalDate fromDate,
                                          @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(toGroupMetric(insightService.groupMetrics(groupId, fromDate, toDate)));
    }

    @GetMapping("/groups/compare")
    @Operation(summary = "So sánh nhiều nhóm khách hàng")
    public ResponseEntity<List<GroupMetricResponse>> compareGroups(@RequestParam List<Long> groupIds,
                                           @RequestParam(required = false) LocalDate fromDate,
                                           @RequestParam(required = false) LocalDate toDate) {
        long distinctGroupCount = groupIds.stream().distinct().count();
        if (distinctGroupCount < 2 || distinctGroupCount > 20) {
            throw new IllegalArgumentException("groupIds cần có từ 2 đến 20 nhóm");
        }
        return ResponseEntity.ok(groupIds.stream()
                .distinct()
                .map(id -> toGroupMetric(insightService.groupMetrics(id, fromDate, toDate)))
                .toList());
    }

    @GetMapping("/customers/{customerId}/period-comparison")
    @Operation(summary = "So sánh hành vi mua của khách giữa hai giai đoạn")
    public ResponseEntity<PeriodComparisonResponse> compareCustomerPeriods(@PathVariable Long customerId,
                                                     @RequestParam LocalDate firstFrom,
                                                     @RequestParam LocalDate firstTo,
                                                     @RequestParam LocalDate secondFrom,
                                                     @RequestParam LocalDate secondTo) {
        var result = insightService.compareCustomerPeriods(customerId, firstFrom, firstTo, secondFrom, secondTo);
        return ResponseEntity.ok(new PeriodComparisonResponse(result.customerId(),
                toPeriodMetrics(result.firstPeriod()), toPeriodMetrics(result.secondPeriod())));
    }

    @GetMapping("/sales-trends")
    @Operation(summary = "Xu hướng doanh thu và đơn hàng theo tháng")
    public ResponseEntity<List<SalesTrendResponse>> salesTrends(@RequestParam(required = false) LocalDate fromDate,
                                         @RequestParam(required = false) LocalDate toDate,
                                         @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(insightService.monthlySales(fromDate, toDate, period).stream()
                .map(row -> new SalesTrendResponse(row.month(), row.totalOrders(), row.activeCustomers(), row.revenue()))
                .toList());
    }

    @GetMapping("/feedback-summary")
    @Operation(summary = "Tổng hợp phản hồi theo loại, cảm xúc và trạng thái")
    public ResponseEntity<List<FeedbackSummaryResponse>> feedbackSummary(@RequestParam(required = false) LocalDate fromDate,
                                             @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(insightService.feedbackSummary(fromDate, toDate).stream()
                .map(row -> new FeedbackSummaryResponse(row.issueType(), row.category(), row.sentiment(), row.status(),
                        row.feedbackCount(), row.customerCount())).toList());
    }

    @GetMapping("/feedback-trends")
    @Operation(summary = "Xu hướng số lượng phản hồi, khiếu nại theo ngày, tuần hoặc tháng")
    public ResponseEntity<List<FeedbackTrendResponse>> feedbackTrends(@RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(insightService.feedbackTrends(fromDate, toDate, period).stream()
                .map(row -> new FeedbackTrendResponse(row.periodStart(), row.totalFeedback(), row.complaints(),
                        row.suggestions(), row.praises(), row.negativeFeedback(), row.unresolvedFeedback(), row.customerCount()))
                .toList());
    }

    @GetMapping("/customer-growth")
    @Operation(summary = "Báo cáo tăng trưởng hồ sơ và khách mua hàng")
    public ResponseEntity<List<CustomerGrowthResponse>> customerGrowth(@RequestParam(required = false) LocalDate fromDate,
                                            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(insightService.customerGrowth(fromDate, toDate).stream()
                .map(row -> new CustomerGrowthResponse(row.month(), row.newProfiles(), row.activeCustomers(),
                        row.firstTimeBuyers(), row.returningCustomers())).toList());
    }

    @GetMapping("/customer-value")
    @Operation(summary = "Giá trị, tần suất, độ gắn bó và lần mua gần nhất của khách")
    public ResponseEntity<List<CustomerValueResponse>> customerValue(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(insightService.customerValue(limit).stream()
                .map(row -> new CustomerValueResponse(row.customerId(), row.fullName(), row.phone(), row.loyaltyPoints(),
                        row.registeredAt(), row.totalSpend(), row.totalOrders(), row.averageOrderValue(),
                        row.lastOrderAt(), row.daysSinceLastOrder(), row.customerTenureDays())).toList());
    }

    @GetMapping("/product-affinity")
    @Operation(summary = "Sản phẩm hoặc danh mục được mua nhiều nhất")
    public ResponseEntity<List<ProductAffinityResponse>> productAffinity(@RequestParam(defaultValue = "coffee") String dimension,
                                             @RequestParam(required = false) LocalDate fromDate,
                                             @RequestParam(required = false) LocalDate toDate,
                                             @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(insightService.productAffinity(dimension, fromDate, toDate, limit).stream()
                .map(row -> new ProductAffinityResponse(row.itemId(), row.itemName(), row.customerCount(), row.orderCount(),
                        row.quantitySold(), row.revenue())).toList());
    }

    @GetMapping("/repeated-complaints")
    @Operation(summary = "Các chủ đề khiếu nại xuất hiện lặp lại")
    public ResponseEntity<List<RepeatedComplaintResponse>> repeatedComplaints(@RequestParam(required = false) LocalDate fromDate,
                                                @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(insightService.repeatedComplaints(fromDate, toDate).stream()
                .map(row -> new RepeatedComplaintResponse(row.topic(), row.complaintCount(), row.customerCount())).toList());
    }

    @GetMapping(value = "/top-customers/export", produces = "text/csv")
    @Operation(summary = "Xuất bảng xếp hạng khách hàng ra CSV")
    public ResponseEntity<byte[]> exportTopCustomers(@RequestParam(required = false) LocalDate fromDate,
                                                       @RequestParam(required = false) LocalDate toDate,
                                                       @RequestParam(defaultValue = "500") int limit,
                                                       @RequestParam(defaultValue = "spend") String sortBy) {
        List<CustomerInsightRepository.CustomerMetricRow> customers = insightService.topCustomers(fromDate, toDate, limit, sortBy);
        StringBuilder csv = new StringBuilder("customer_id,full_name,phone,total_spend,total_orders,average_order_value,last_order_at,days_since_last_order\r\n");
        for (var customer : customers) {
            csv.append(customer.customerId()).append(',')
                    .append(csv(customer.fullName())).append(',')
                    .append(csv(customer.phone())).append(',')
                    .append(customer.totalSpend()).append(',')
                    .append(customer.totalOrders()).append(',')
                    .append(customer.averageOrderValue()).append(',')
                    .append(customer.lastOrderAt() == null ? "" : customer.lastOrderAt()).append(',')
                    .append(customer.daysSinceLastOrder() == null ? "" : customer.daysSinceLastOrder()).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer-insights.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csv(String value) {
        if (value == null) return "";
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static CustomerMetricResponse toCustomerMetric(CustomerInsightRepository.CustomerMetricRow row) {
        return new CustomerMetricResponse(row.customerId(), row.fullName(), row.phone(), row.totalSpend(),
                row.totalOrders(), row.averageOrderValue(), row.lastOrderAt(), row.daysSinceLastOrder());
    }

    private static GroupMetricResponse toGroupMetric(CustomerInsightRepository.GroupMetricRow row) {
        return new GroupMetricResponse(row.groupId(), row.groupName(), row.memberCount(), row.totalSpend(),
                row.totalOrders(), row.averageOrderValue(), row.favoriteCategory(), row.favoriteCoffee(),
                row.interactionCount(), row.feedbackCount());
    }

    private static PeriodMetricsResponse toPeriodMetrics(CustomerInsightService.PeriodMetrics metrics) {
        return new PeriodMetricsResponse(metrics.totalSpend(), metrics.totalOrders(),
                metrics.averageOrderValue(), metrics.lastOrderAt());
    }
}
