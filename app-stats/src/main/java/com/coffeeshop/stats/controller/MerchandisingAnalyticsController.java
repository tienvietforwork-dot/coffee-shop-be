package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.service.MerchandisingAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchandising-analytics")
public class MerchandisingAnalyticsController {
    private final MerchandisingAnalyticsService service;
    @GetMapping("/report")
    public Map<String,Object> report(@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return service.report(from,to);
    }
    private String csv(Object x) {
        String s=x==null?"":x.toString();
        if (s.matches("^[=+@-].*")) s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    private void appendCsvRow(StringBuilder csv, Object... values) {
        for (int i=0;i<values.length;i++) {
            if (i>0) csv.append(',');
            csv.append(csv(values[i]));
        }
        csv.append("\r\n");
    }
    @GetMapping(value="/export",produces="text/csv")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate from,@RequestParam LocalDate to) {
        Map<String,Object> data=service.report(from,to);
        StringBuilder csv=new StringBuilder("section,id,name,orders,completed_orders,units_sold,revenue,aov,impressions,clicks,accepts,add_to_cart\r\n");
        @SuppressWarnings("unchecked") Map<String,Object> orders=(Map<String,Object>)data.get("orders");
        appendCsvRow(csv,"orders","","Tổng quan",
                orders.getOrDefault("orders",0),0,0,orders.getOrDefault("revenue",0),orders.getOrDefault("aov",0),0,0,0,0);
        for (String key:List.of("products","bundles","crossSell","upsell","recommendations")) {
            @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)data.get(key);
            for (var row:rows) {
                Object id=row.getOrDefault("coffee_id",row.getOrDefault("bundle_id",row.get("rule_id")));
                Object name=row.getOrDefault("coffee_name",row.getOrDefault("bundle_name",row.get("rule_name")));
                appendCsvRow(csv,key,id,name,
                        row.getOrDefault("orders",0),row.getOrDefault("completed_orders",0),row.getOrDefault("units_sold",0),
                        row.getOrDefault("line_revenue",row.getOrDefault("revenue",0)),row.getOrDefault("aov",0),
                        row.getOrDefault("impressions",0),row.getOrDefault("clicks",0),
                        row.getOrDefault("accepts",0),row.getOrDefault("add_to_cart",0));
            }
        }
        byte[] bytes=("\uFEFF"+csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=merchandising-analytics.csv")
                .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(bytes);
    }
}
