package com.coffeeshop.integration.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thời tiết tại quán qua Open-Meteo (miễn phí, không cần key): hiện tại và dự báo theo giờ 2 ngày tới (giờ Việt Nam).
 * Kết quả nhớ 15 phút cho mỗi vị trí. Lỗi mạng = không biết thời tiết (null): quán không thu phụ phí, không ngưng giao.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherGateway {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** {@code time}: giờ VN; {@code rainMmPerHour}: lượng mưa quy ra mm / giờ; {@code code}: mã WMO (95–99 = giông) */
    public record Weather(LocalDateTime time, int code, double temperature, double rainMmPerHour, double windKmh) {
        public boolean storm() {
            return code >= 95;
        }

        /** mô tả tiếng Việt theo mã WMO, kèm lượng mưa khi có */
        public String describe() {
            String sky = describe(code);
            return rainMmPerHour > 0 && !storm() ? String.format(new Locale("vi", "VN"), "%s %.1f mm/h", sky, rainMmPerHour) : sky;
        }

        public static String describe(int code) {
            return switch (code) {
                case 0 -> "Trời quang";
                case 1, 2 -> "Ít mây";
                case 3 -> "Nhiều mây";
                case 45, 48 -> "Sương mù";
                case 51, 53, 55, 56, 57 -> "Mưa phùn";
                case 61, 80 -> "Mưa nhẹ";
                case 63, 81 -> "Mưa vừa";
                case 65, 82 -> "Mưa to";
                case 66, 67 -> "Mưa lạnh";
                case 95 -> "Giông";
                case 96, 99 -> "Giông, mưa đá";
                default -> code >= 71 && code <= 77 ? "Tuyết" : "Không rõ";
            };
        }
    }

    private record Cached(Instant at, JsonNode body) {
    }

    private final ObjectMapper json;
    private final RestClient http = RestClient.create();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Value("${app.delivery.weather.url:https://api.open-meteo.com/v1/forecast}")
    private String url;

    /** thời tiết lúc {@code at} (giờ VN; null hoặc trong 1 giờ tới = hiện tại) tại (lat, lng), hoặc null khi không lấy được */
    public Weather at(double lat, double lng, LocalDateTime at) {
        JsonNode body = forecast(lat, lng);
        if (body == null) return null;
        if (at == null || Math.abs(ChronoUnit.MINUTES.between(LocalDateTime.now(ZONE), at)) <= 60) return current(body);
        String hour = at.truncatedTo(ChronoUnit.HOURS).toString(); // "2026-11-06T18:00"
        JsonNode times = body.path("hourly").path("time");
        for (int i = 0; i < times.size(); i++) {
            if (times.get(i).asText().equals(hour)) return hourly(body, i);
        }
        return null; // beyond the 2-day forecast
    }

    /** hiện tại + {@code hours} giờ tới, hoặc danh sách rỗng khi không lấy được */
    public List<Weather> outlook(double lat, double lng, int hours) {
        JsonNode body = forecast(lat, lng);
        List<Weather> out = new ArrayList<>();
        if (body == null) return out;
        Weather now = current(body);
        if (now != null) out.add(now);
        String next = LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.HOURS).plusHours(1).toString();
        JsonNode times = body.path("hourly").path("time");
        for (int i = 0; i < times.size() && out.size() <= hours; i++) {
            if (times.get(i).asText().compareTo(next) >= 0) out.add(hourly(body, i));
        }
        return out;
    }

    private static Weather current(JsonNode body) {
        JsonNode c = body.path("current");
        if (c.isMissingNode()) return null;
        double interval = c.path("interval").asDouble(900); // precipitation is over the last interval (seconds)
        return new Weather(LocalDateTime.parse(c.path("time").asText()), c.path("weather_code").asInt(),
                c.path("temperature_2m").asDouble(), c.path("precipitation").asDouble() * 3600 / interval, c.path("wind_speed_10m").asDouble());
    }

    private static Weather hourly(JsonNode body, int i) {
        JsonNode h = body.path("hourly");
        return new Weather(LocalDateTime.parse(h.path("time").get(i).asText()), h.path("weather_code").get(i).asInt(),
                h.path("temperature_2m").get(i).asDouble(), h.path("precipitation").get(i).asDouble(), h.path("wind_speed_10m").get(i).asDouble());
    }

    private JsonNode forecast(double lat, double lng) {
        String key = String.format(Locale.ROOT, "%.2f,%.2f", lat, lng);
        Cached c = cache.get(key);
        if (c != null && c.at().isAfter(Instant.now().minusSeconds(900))) return c.body();
        try {
            String vars = "temperature_2m,precipitation,weather_code,wind_speed_10m";
            String raw = http.get().uri(java.net.URI.create(String.format(Locale.ROOT, "%s?latitude=%.4f&longitude=%.4f&current=%s&hourly=%s"
                    + "&timezone=Asia%%2FHo_Chi_Minh&forecast_days=2", url, lat, lng, vars, vars))).retrieve().body(String.class);
            JsonNode body = json.readTree(raw == null ? "{}" : raw);
            cache.put(key, new Cached(Instant.now(), body));
            return body;
        } catch (Exception e) {
            log.warn("Open-Meteo failed: {}", e.getMessage());
            return c == null ? null : c.body(); // stale beats nothing
        }
    }
}
