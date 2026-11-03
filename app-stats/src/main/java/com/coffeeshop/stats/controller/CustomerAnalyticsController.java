package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.service.CustomerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-analytics")
@RequiredArgsConstructor
public class CustomerAnalyticsController {
    private final CustomerAnalyticsService service;

    @GetMapping("/trends")
    public List<Map<String, Object>> trends(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.trends(from, to);
    }
    @GetMapping("/spending")
    public List<Map<String, Object>> spending(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.spending(from, to);
    }
    @GetMapping("/groups")
    public List<Map<String, Object>> groups(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.groups(from, to);
    }
    @GetMapping("/growth")
    public List<Map<String, Object>> growth(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.growth(from, to);
    }
    @GetMapping("/report")
    public Map<String, Object> report(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.report(from, to);
    }

    private String csv(Object value) {
        String str = value == null ? "" : value.toString();
        if (str.matches("^[=+@-].*")) str = "'" + str; // avoid spreadsheet formula injection
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }
    private void section(StringBuilder b, String title, List<Map<String, Object>> rows, String... keys) {
        b.append(csv(title)).append("\r\n");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) b.append(','); b.append(csv(keys[i]));
        }
        b.append("\r\n");
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) b.append(','); b.append(csv(row.get(keys[i])));
            }
            b.append("\r\n");
        }
        b.append("\r\n");
    }
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        // Validate dates before streaming anything.
        var report = service.report(from, to);
        StringBuilder b = new StringBuilder("\uFEFF");
        b.append("\"Customer analytics\",\"" + from + "\",\"" + to + "\"\r\n\r\n");
        section(b, "Purchase trends", service.trends(from, to), "month", "orders", "buyers", "revenue", "avg_order_value");
        section(b, "Customer spending (top 500)", service.spending(from, to), "customer_id", "customer_name", "phone", "orders", "total_spent", "avg_order_value", "last_order_at");
        section(b, "Group comparison (overlaps possible)", service.groups(from, to), "group_id", "group_name", "members", "buying_customers", "orders", "revenue", "avg_order_value");
        section(b, "Customer growth", service.growth(from, to), "month", "new_customers", "converted_customers");
        b.append(csv(report.get("note"))).append("\r\n");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer-analytics-" + from + "-" + to + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(b.toString().getBytes(StandardCharsets.UTF_8));
    }
}
