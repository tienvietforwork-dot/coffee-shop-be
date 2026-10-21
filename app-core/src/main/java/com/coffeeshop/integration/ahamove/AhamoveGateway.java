package com.coffeeshop.integration.ahamove;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ahamove partner API v3 (developers.ahamove.com): báo giá {@code POST /orders/estimates}, tạo đơn {@code POST /orders},
 * hủy {@code DELETE /orders/{id}} (only while IDLE / ASSIGNING / ACCEPTED). The path is [shop → customer]; the trip's
 * progress comes back on the webhook. Statuses: IDLE, ASSIGNING, ACCEPTED, CONFIRMING, IN PROCESS, COMPLETED, CANCELLED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AhamoveGateway {

    private final AhamoveProperties props;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    public record Estimate(BigDecimal fee, BigDecimal distanceKm, Integer durationMinutes, String serviceId) {
    }

    public record Booking(String orderId, String status, BigDecimal fee, String trackingUrl, BigDecimal distanceKm) {
    }

    public boolean simulated() {
        return props.simulated();
    }

    public AhamoveProperties props() {
        return props;
    }

    public Estimate estimate(String dropAddress, String name, String phone, BigDecimal cod) {
        if (props.simulated()) {
            // distance from the address text so the same address always gets the same price: 1.5 – 9.5 km
            double km = 1.5 + Math.floorMod(dropAddress.toLowerCase().hashCode(), 80) / 10.0;
            return new Estimate(simulatedFee(km), BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP),
                    (int) Math.round(10 + km * 4), props.getServiceId());
        }
        Map<String, Object> body = order(dropAddress, name, phone, cod, null);
        body.put("services", List.of(Map.of("_id", props.getServiceId())));
        JsonNode r = post("/orders/estimates", body);
        JsonNode first = r.isArray() ? r.get(0) : r;
        JsonNode data = first.has("data") ? first.get("data") : first;
        return new Estimate(dec(data.path("total_price")), dec(data.path("distance")),
                data.path("duration").isMissingNode() ? null : (int) Math.round(data.path("duration").asDouble() / 60), props.getServiceId());
    }

    public Booking create(String trackingNumber, String dropAddress, String name, String phone, BigDecimal cod, String remarks) {
        if (props.simulated()) {
            Estimate e = estimate(dropAddress, name, phone, cod);
            String id = "SIM" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
            return new Booking(id, "ASSIGNING", e.fee(), "https://cloudjetstg.ahamove.com/share-order/" + id, e.distanceKm());
        }
        Map<String, Object> body = order(dropAddress, name, phone, cod, remarks);
        body.put("service_id", props.getServiceId());
        body.put("tracking_number", trackingNumber);
        JsonNode r = post("/orders", body);
        return new Booking(r.path("order_id").asText(), r.path("status").asText("ASSIGNING"),
                dec(r.path("order").path("total_pay")), r.path("shared_link").asText(null),
                dec(r.path("order").path("distance")));
    }

    public void cancel(String orderId, String comment) {
        if (props.simulated()) return;
        try {
            http.method(org.springframework.http.HttpMethod.DELETE).uri(props.getBaseUrl() + "/orders/" + orderId)
                    .header("Authorization", "Bearer " + props.getToken()).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("comment", comment)).retrieve().toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalStateException("Ahamove không cho hủy chuyến: " + e.getMessage(), e);
        }
    }

    /** Ahamove-like price list for the simulation: 16.000đ first 2 km, then 5.000đ / km */
    private static BigDecimal simulatedFee(double km) {
        double fee = 16000 + Math.max(0, km - 2) * 5000;
        return BigDecimal.valueOf(Math.round(fee / 1000.0) * 1000L).setScale(2, RoundingMode.UNNECESSARY);
    }

    private Map<String, Object> order(String dropAddress, String name, String phone, BigDecimal cod, String remarks) {
        Map<String, Object> pickup = new LinkedHashMap<>();
        pickup.put("lat", props.getShopLat());
        pickup.put("lng", props.getShopLng());
        pickup.put("address", props.getShopAddress());
        pickup.put("name", props.getShopName());
        pickup.put("mobile", props.getShopPhone());
        Map<String, Object> drop = new LinkedHashMap<>();
        drop.put("address", dropAddress);
        drop.put("name", name);
        drop.put("mobile", phone);
        if (cod != null && cod.signum() > 0) drop.put("cod", cod.longValue());
        if (remarks != null) drop.put("remarks", remarks);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_time", 0);
        body.put("path", List.of(pickup, drop));
        body.put("payment_method", "BALANCE");
        return body;
    }

    private JsonNode post(String path, Object body) {
        try {
            String raw = http.post().uri(props.getBaseUrl() + path).header("Authorization", "Bearer " + props.getToken())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            return json.readTree(raw);
        } catch (Exception e) {
            log.warn("Ahamove {} failed: {}", path, e.getMessage());
            throw new IllegalStateException("Ahamove lỗi: " + e.getMessage(), e);
        }
    }

    private static BigDecimal dec(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? null : new BigDecimal(n.asText());
    }
}
