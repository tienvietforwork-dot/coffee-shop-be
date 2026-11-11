package com.coffeeshop.integration.distance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Địa chỉ → toạ độ (geocode) và quãng đường đi xe quán → khách, qua Goong.io hoặc OpenStreetMap (Nominatim + OSRM).
 * Kết quả geocode được nhớ trong bộ nhớ: báo giá gọi lại nhiều lần cho cùng một địa chỉ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistanceGateway {

    public record Point(double lat, double lng) {
    }

    /** quãng đường đường bộ (km) và thời gian đi (phút, có thể null) */
    public record Route(double km, Integer minutes) {
    }

    private static final int CACHE_MAX = 2000;

    private final DistanceProperties props;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();
    private final Map<String, Point> geocodeCache = new ConcurrentHashMap<>();
    private final Map<String, Route> routeCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> suggestCache = new ConcurrentHashMap<>();

    public String provider() {
        return props.effectiveMode();
    }

    /** toạ độ của địa chỉ; IllegalStateException khi không tìm thấy */
    public Point geocode(String address, Point near) {
        String key = norm(address);
        Point cached = geocodeCache.get(key);
        if (cached != null) return cached;
        Point p = switch (provider()) {
            case "simulated" -> simulatedPoint(address, near);
            case "google" -> googleGeocode(address);
            case "goong" -> goongGeocode(address);
            default -> osmGeocode(address, near);
        };
        if (p == null) throw new IllegalStateException("Không tìm thấy địa chỉ trên bản đồ — chọn địa chỉ trong danh sách gợi ý");
        if (geocodeCache.size() > CACHE_MAX) geocodeCache.clear();
        geocodeCache.put(key, p);
        return p;
    }

    /** quãng đường đi xe máy từ {@code from} tới {@code to} */
    public Route route(Point from, Point to) {
        String key = String.format(Locale.ROOT, "%.5f,%.5f>%.5f,%.5f", from.lat(), from.lng(), to.lat(), to.lng());
        Route cached = routeCache.get(key);
        if (cached != null) return cached;
        Route r = null;
        try {
            r = switch (provider()) {
                case "simulated" -> null;
                case "google" -> googleRoute(from, to);
                case "goong" -> goongRoute(from, to);
                default -> osrmRoute(from, to);
            };
        } catch (Exception e) {
            log.warn("routing {} failed, falling back to straight line × 1.3: {}", provider(), e.getMessage());
        }
        if (r == null) {
            double km = haversineKm(from, to) * 1.3; // đường thực tế ≈ 1,3 × đường chim bay
            r = new Route(km, (int) Math.round(5 + km * 3));
        }
        if (routeCache.size() > CACHE_MAX) routeCache.clear();
        routeCache.put(key, r);
        return r;
    }

    /**
     * Gợi ý địa chỉ khi gõ (như ô tìm kiếm Google Maps), ưu tiên quanh {@code near}. Gợi ý có sẵn toạ độ (Photon) được nhớ
     * lại, để chọn gợi ý rồi đo km dùng đúng điểm đó.
     */
    public List<String> suggest(String query, Point near) {
        return suggest(query, near, null, null);
    }

    /**
     * Gợi ý trong phạm vi phường / tỉnh khách đã chọn. OpenStreetMap: lấy ranh giới phường (Nominatim) và chỉ giữ gợi ý
     * nằm trong ranh giới đó; nhãn chỉ còn số nhà / đường (phường, tỉnh đã chọn ở ô riêng). Toạ độ của gợi ý được nhớ cho
     * địa chỉ đầy đủ "đường, phường, tỉnh" để đo km đúng điểm đó. {@code ward} / {@code province} null = không giới hạn.
     */
    public List<String> suggest(String query, Point near, String ward, String province) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 3) return List.of();
        String area = String.join(", ", java.util.stream.Stream.of(ward, province).filter(s -> s != null && !s.isBlank()).toList());
        Area shape = ward == null || ward.isBlank() || !"osm".equals(provider()) ? null : areaOf(area);
        Point center = shape != null ? shape.center() : area.isEmpty() ? near : areaCenter(ward, province, near);
        String key = norm(q) + "|" + norm(area) + (center == null ? "" : "@" + Math.round(center.lat() * 100) + "," + Math.round(center.lng() * 100));
        List<String> cached = suggestCache.get(key);
        if (cached != null) return cached;
        List<String> out;
        String full = area.isEmpty() ? q : q + ", " + area;
        try {
            out = switch (provider()) {
                case "simulated" -> List.of(q);
                case "google" -> googleSuggest(full, center, area.isEmpty() ? 30000.0 : 15000.0);
                case "goong" -> goongSuggest(full, center);
                default -> shape != null ? photonInArea(q, shape, area) : photonSuggest(q, center, !area.isEmpty());
            };
        } catch (IllegalStateException e) {
            return List.of();
        }
        if (shape == null && province != null && !province.isBlank()) {
            String pk = plain(province);
            out = out.stream().filter(s -> plain(s).contains(pk)).toList();
        }
        if (suggestCache.size() > CACHE_MAX) suggestCache.clear();
        suggestCache.put(key, out);
        return out;
    }

    /** ranh giới phường: tâm, khung bao [minLat, maxLat, minLon, maxLon] và các vòng ngoài [lon, lat] */
    record Area(Point center, double[] bbox, List<double[][]> rings) {
        boolean contains(double lat, double lon) {
            if (lat < bbox[0] || lat > bbox[1] || lon < bbox[2] || lon > bbox[3]) return false;
            for (double[][] ring : rings) {
                boolean in = false;
                for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
                    double xi = ring[i][0], yi = ring[i][1], xj = ring[j][0], yj = ring[j][1];
                    if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) in = !in;
                }
                if (in) return true;
            }
            return false;
        }
    }

    private final Map<String, java.util.Optional<Area>> areaCache = new ConcurrentHashMap<>();

    /** ranh giới hành chính của "Phường …, Tỉnh …" từ Nominatim (null khi OSM chưa có) */
    /**
     * Khoảng cách đường chim bay (km) từ {@code from} tới điểm gần nhất của phường (0 khi {@code from} nằm trong phường),
     * hoặc null khi không có ranh giới. Đường đi thật luôn dài hơn, nên vượt ngưỡng ở đây = chắc chắn ngoài phạm vi.
     */
    public Double nearestKm(String ward, String province, Point from) {
        if (ward == null || ward.isBlank()) return null;
        Area a = areaOf(ward + (province == null || province.isBlank() ? "" : ", " + province));
        if (a == null) return null;
        if (a.contains(from.lat(), from.lng())) return 0.0;
        double best = Double.MAX_VALUE;
        for (double[][] ring : a.rings()) {
            for (double[] p : ring) best = Math.min(best, haversineKm(from, new Point(p[1], p[0])));
        }
        return best;
    }

    private Area areaOf(String area) {
        return areaCache.computeIfAbsent(norm(area), k -> {
            try {
                throttleNominatim();
                JsonNode r = getJson(UriComponentsBuilder.fromHttpUrl(props.getNominatimUrl() + "/search")
                        .queryParam("q", area).queryParam("format", "json").queryParam("limit", 1).queryParam("countrycodes", "vn")
                        .queryParam("polygon_geojson", 1).queryParam("polygon_threshold", 0.0003).encode().build().toUri()).path(0);
                JsonNode g = r.path("geojson");
                List<double[][]> rings = new ArrayList<>();
                if ("Polygon".equals(g.path("type").asText())) rings.add(ring(g.path("coordinates").path(0)));
                else if ("MultiPolygon".equals(g.path("type").asText())) g.path("coordinates").forEach(p -> rings.add(ring(p.path(0))));
                JsonNode bb = r.path("boundingbox");
                if (rings.isEmpty() || bb.size() != 4) return java.util.Optional.<Area>empty();
                return java.util.Optional.of(new Area(new Point(r.path("lat").asDouble(), r.path("lon").asDouble()),
                        new double[]{bb.get(0).asDouble(), bb.get(1).asDouble(), bb.get(2).asDouble(), bb.get(3).asDouble()}, rings));
            } catch (IllegalStateException e) {
                return java.util.Optional.<Area>empty();
            }
        }).orElse(null);
    }

    private static double[][] ring(JsonNode coords) {
        double[][] out = new double[coords.size()][];
        for (int i = 0; i < coords.size(); i++) out[i] = new double[]{coords.get(i).get(0).asDouble(), coords.get(i).get(1).asDouble()};
        return out;
    }

    /** Photon trong khung bao của phường, chỉ giữ điểm nằm trong ranh giới; nhãn = số nhà / đường */
    private List<String> photonInArea(String q, Area a, String areaLabel) {
        URI uri = UriComponentsBuilder.fromHttpUrl(props.getPhotonUrl() + "/api/").queryParam("q", q).queryParam("limit", 30)
                .queryParam("lat", a.center().lat()).queryParam("lon", a.center().lng())
                .queryParam("bbox", String.format(Locale.ROOT, "%.5f,%.5f,%.5f,%.5f", a.bbox()[2] - 0.002, a.bbox()[0] - 0.002, a.bbox()[3] + 0.002, a.bbox()[1] + 0.002))
                .encode().build().toUri();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (JsonNode f : getJson(uri).path("features")) {
            JsonNode c = f.path("geometry").path("coordinates");
            if (!c.isArray() || c.size() != 2) continue;
            double lon = c.get(0).asDouble(), lat = c.get(1).asDouble();
            if (!a.contains(lat, lon)) continue;
            JsonNode p = f.path("properties");
            String num = p.path("housenumber").asText("");
            String street = p.path("street").asText("");
            String name = p.path("name").asText("");
            List<String> parts = new ArrayList<>();
            if (!name.isBlank() && !name.equals(street)) parts.add(name);
            if (!street.isBlank()) parts.add((num.isBlank() ? "" : num + " ") + street);
            else if (!num.isBlank() && !parts.isEmpty()) parts.set(0, num + " " + parts.get(0));
            if (parts.isEmpty()) continue;
            String label = String.join(", ", parts);
            // the form sends "label, Phường …, Tỉnh …": measure from this exact point
            geocodeCache.putIfAbsent(norm(label + ", " + areaLabel), new Point(lat, lon));
            out.add(label);
            if (out.size() == 6) break;
        }
        return new ArrayList<>(out);
    }

    // ---- Google Maps Platform -----------------------------------------------------

    /** tâm phường (hoặc tỉnh) đã chọn, để gợi ý quanh đó */
    private Point areaCenter(String ward, String province, Point fallback) {
        for (String a : java.util.Arrays.asList(ward == null ? null : ward + ", " + province, province)) {
            if (a == null || a.isBlank()) continue;
            try {
                return geocode(a, null);
            } catch (IllegalStateException e) {
                log.debug("no centre for {}: {}", a, e.getMessage());
            }
        }
        return fallback;
    }

    /** lower case, no accents, no "Thành phố / Tỉnh / Phường / Xã" prefix: "Thành phố Hà Nội" ~ "Hà Nội" ~ "ha noi" */
    static String plain(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT)
                .replaceAll("\\b(thanh pho|tinh|phuong|xa|dac khu)\\s+", "").replace("ho chi minh city", "ho chi minh")
                .replaceAll("\\s+", " ").trim();
    }

    private List<String> googleSuggest(String q, Point near, double radius) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("input", q);
        body.put("languageCode", "vi");
        body.put("includedRegionCodes", List.of("vn"));
        if (near != null) body.put("locationBias", Map.of("circle", Map.of("center", Map.of("latitude", near.lat(), "longitude", near.lng()), "radius", radius)));
        JsonNode r;
        try {
            String raw = http.post().uri("https://places.googleapis.com/v1/places:autocomplete")
                    .header("X-Goog-Api-Key", props.getGoogleApiKey()).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);
            r = json.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.warn("Google autocomplete failed: {}", e.getMessage());
            throw new IllegalStateException("Dịch vụ bản đồ không phản hồi", e);
        }
        List<String> out = new ArrayList<>();
        r.path("suggestions").forEach(s -> {
            String text = s.path("placePrediction").path("text").path("text").asText("");
            if (!text.isBlank()) out.add(text);
        });
        return out;
    }

    private Point googleGeocode(String address) {
        JsonNode r = getJson(UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address).queryParam("region", "vn").queryParam("language", "vi")
                .queryParam("key", props.getGoogleApiKey()).encode().build().toUri());
        JsonNode loc = r.path("results").path(0).path("geometry").path("location");
        return loc.isMissingNode() ? null : new Point(loc.path("lat").asDouble(), loc.path("lng").asDouble());
    }

    private Route googleRoute(Point from, Point to) {
        JsonNode r = getJson(UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/distancematrix/json")
                .queryParam("origins", from.lat() + "," + from.lng()).queryParam("destinations", to.lat() + "," + to.lng())
                .queryParam("mode", "driving").queryParam("key", props.getGoogleApiKey()).encode().build().toUri());
        JsonNode el = r.path("rows").path(0).path("elements").path(0);
        if (!"OK".equalsIgnoreCase(el.path("status").asText("")) || el.path("distance").isMissingNode()) return null;
        return new Route(el.path("distance").path("value").asDouble() / 1000.0,
                (int) Math.round(el.path("duration").path("value").asDouble() / 60.0));
    }

    // ---- Goong.io ---------------------------------------------------------------

    private List<String> goongSuggest(String q, Point near) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(props.getGoongBaseUrl() + "/Place/AutoComplete")
                .queryParam("input", q).queryParam("limit", 6).queryParam("api_key", props.getGoongApiKey());
        if (near != null) b.queryParam("location", near.lat() + "," + near.lng());
        List<String> out = new ArrayList<>();
        getJson(b.encode().build().toUri()).path("predictions").forEach(p -> {
            String text = p.path("description").asText("");
            if (!text.isBlank()) out.add(text);
        });
        return out;
    }

    private Point goongGeocode(String address) {
        JsonNode r = getJson(UriComponentsBuilder.fromHttpUrl(props.getGoongBaseUrl() + "/geocode")
                .queryParam("address", address).queryParam("api_key", props.getGoongApiKey()).encode().build().toUri());
        JsonNode loc = r.path("results").path(0).path("geometry").path("location");
        return loc.isMissingNode() ? null : new Point(loc.path("lat").asDouble(), loc.path("lng").asDouble());
    }

    private Route goongRoute(Point from, Point to) {
        JsonNode r = getJson(UriComponentsBuilder.fromHttpUrl(props.getGoongBaseUrl() + "/DistanceMatrix")
                .queryParam("origins", from.lat() + "," + from.lng()).queryParam("destinations", to.lat() + "," + to.lng())
                .queryParam("vehicle", "bike").queryParam("api_key", props.getGoongApiKey()).encode().build().toUri());
        JsonNode el = r.path("rows").path(0).path("elements").path(0);
        if (!"OK".equalsIgnoreCase(el.path("status").asText("OK")) || el.path("distance").isMissingNode()) return null;
        return new Route(el.path("distance").path("value").asDouble() / 1000.0,
                (int) Math.round(el.path("duration").path("value").asDouble() / 60.0));
    }

    // ---- OpenStreetMap -------------------------------------------------------------

    /** Photon (komoot): tìm theo từng chữ gõ, kèm toạ độ — được nhớ vào geocode cache theo nhãn hiển thị */
    private List<String> photonSuggest(String q, Point near, boolean onlyNearby) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(props.getPhotonUrl() + "/api/")
                .queryParam("q", q).queryParam("limit", 15);
        if (near != null) {
            b.queryParam("lat", near.lat()).queryParam("lon", near.lng());
            // the chosen ward: only ~15 km around it
            if (onlyNearby) b.queryParam("bbox", String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f",
                    near.lng() - 0.15, near.lat() - 0.15, near.lng() + 0.15, near.lat() + 0.15));
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (JsonNode f : getJson(b.encode().build().toUri()).path("features")) {
            JsonNode p = f.path("properties");
            if (!"VN".equalsIgnoreCase(p.path("countrycode").asText(""))) continue;
            List<String> parts = new ArrayList<>();
            String num = p.path("housenumber").asText("");
            String street = p.path("street").asText("");
            String name = p.path("name").asText("");
            if (!name.isBlank() && !name.equals(street)) parts.add(name);
            if (!street.isBlank()) parts.add((num.isBlank() ? "" : num + " ") + street);
            else if (!num.isBlank() && !parts.isEmpty()) parts.set(0, num + " " + parts.get(0));
            for (String k : List.of("district", "city", "state")) {
                String v = p.path(k).asText("").replace("Ho Chi Minh City", "Thành phố Hồ Chí Minh").replace("Hanoi", "Hà Nội");
                if (!v.isBlank() && parts.stream().noneMatch(x -> plain(x).equals(plain(v)))) parts.add(v);
            }
            if (parts.isEmpty()) continue;
            String label = String.join(", ", parts);
            JsonNode c = f.path("geometry").path("coordinates");
            if (c.isArray() && c.size() == 2) geocodeCache.putIfAbsent(norm(label), new Point(c.get(1).asDouble(), c.get(0).asDouble()));
            out.add(label);
            if (out.size() == 6) break;
        }
        return new ArrayList<>(out);
    }

    private Point osmGeocode(String address, Point near) {
        for (String q : variants(address)) {
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(props.getNominatimUrl() + "/search")
                    .queryParam("format", "json").queryParam("limit", 1).queryParam("countrycodes", "vn")
                    .queryParam("accept-language", "vi").queryParam("q", q);
            if (near != null) { // ưu tiên kết quả quanh quán (~50 km)
                b.queryParam("viewbox", (near.lng() - 0.5) + "," + (near.lat() + 0.5) + "," + (near.lng() + 0.5) + "," + (near.lat() - 0.5));
            }
            throttleNominatim();
            JsonNode r = getJson(b.encode().build().toUri());
            if (r.isArray() && !r.isEmpty()) {
                return new Point(r.get(0).path("lat").asDouble(), r.get(0).path("lon").asDouble());
            }
        }
        return null;
    }

    /** đường đi xe qua lần lượt các điểm: đường vẽ trên bản đồ + tổng km / phút */
    public record Path(List<Point> line, double km, int minutes) {
    }

    /**
     * Tuyến đường thật qua các điểm theo thứ tự (OSRM, không cần key). Lỗi mạng / quá nhiều điểm: null — khi đó bản đồ
     * nối thẳng các điểm.
     */
    public Path path(List<Point> stops) {
        if (stops.size() < 2 || stops.size() > 25) return null;
        try {
            StringBuilder coords = new StringBuilder();
            for (Point p : stops) {
                if (!coords.isEmpty()) coords.append(';');
                coords.append(String.format(Locale.ROOT, "%.6f,%.6f", p.lng(), p.lat()));
            }
            URI uri = URI.create(props.getOsrmUrl() + "/route/v1/driving/" + coords + "?overview=full&geometries=geojson");
            JsonNode route = getJson(uri).path("routes").path(0);
            if (route.isMissingNode()) return null;
            List<Point> line = new ArrayList<>();
            for (JsonNode c : route.path("geometry").path("coordinates")) line.add(new Point(c.get(1).asDouble(), c.get(0).asDouble()));
            return new Path(line, route.path("distance").asDouble() / 1000.0, (int) Math.round(route.path("duration").asDouble() / 60.0));
        } catch (RuntimeException e) {
            log.warn("OSRM path failed: {}", e.getMessage());
            return null;
        }
    }

    private Route osrmRoute(Point from, Point to) {
        URI uri = URI.create(String.format(Locale.ROOT, "%s/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                props.getOsrmUrl(), from.lng(), from.lat(), to.lng(), to.lat()));
        JsonNode route = getJson(uri).path("routes").path(0);
        if (route.isMissingNode()) return null;
        return new Route(route.path("distance").asDouble() / 1000.0, (int) Math.round(route.path("duration").asDouble() / 60.0));
    }

    /**
     * Nominatim hiểu kém số nhà / hẻm kiểu Việt Nam: thử địa chỉ đầy đủ, rồi bỏ số nhà, rồi bỏ dần phần đầu
     * ("12/3 Lê Lợi, Bến Nghé, Quận 1, HCM" → "Lê Lợi, Bến Nghé, …" → "Bến Nghé, Quận 1, HCM" → …).
     */
    static List<String> variants(String address) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        // "TP. Hồ Chí Minh" / "Tp.HCM" → "Hồ Chí Minh": Nominatim misses the prefixed form
        String a = address.trim().replaceAll("\\s+", " ")
                .replaceAll("(?i)\\b(tp\\.?|thành phố)\\s*(hcm|hồ chí minh)\\b", "Hồ Chí Minh")
                .replaceAll("(?i)\\b(tp\\.?|thành phố|tỉnh)\\s+", "");
        out.add(a);
        String noNumber = a.replaceFirst("^(số\\s*)?[0-9][0-9A-Za-z/\\-]*\\s+", "").replaceFirst("(?i)^(hẻm|ngõ|ngách|kiệt)\\s*[0-9/]+\\s*", "");
        List<String> segs = new ArrayList<>();
        for (String p : noNumber.split(",")) if (!p.isBlank()) segs.add(p.trim());
        out.add(String.join(", ", segs));
        // the old quận / phường names are gone since the 2025 merger: street + city alone usually still matches
        if (segs.size() > 2) out.add(segs.get(0) + ", " + segs.get(segs.size() - 1));
        for (int i = 1; i <= segs.size() - 2; i++) out.add(String.join(", ", segs.subList(i, segs.size())));
        return new ArrayList<>(out);
    }

    // ---- helpers -------------------------------------------------------------------

    private long lastNominatimCall;

    /** Nominatim usage policy: at most 1 request / second */
    private synchronized void throttleNominatim() {
        long wait = lastNominatimCall + 1000 - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastNominatimCall = System.currentTimeMillis();
    }

    private JsonNode getJson(URI uri) {
        try {
            String raw = http.get().uri(uri).header("User-Agent", props.getUserAgent()).retrieve().body(String.class);
            return json.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.warn("distance API {} failed: {}", uri.getHost(), e.getMessage());
            throw new IllegalStateException("Dịch vụ bản đồ không phản hồi, thử lại sau", e);
        }
    }

    /** simulated: a point 0.5 – 9.5 km from the shop, the same for the same address */
    private static Point simulatedPoint(String address, Point near) {
        Point base = near == null ? new Point(10.7769, 106.7009) : near;
        double km = 0.5 + Math.floorMod(norm(address).hashCode(), 91) / 10.0;
        return new Point(base.lat() + km / 1.3 / 111.0, base.lng());
    }

    public static double haversineKm(Point a, Point b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLng = Math.toRadians(b.lng() - a.lng());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * 6371.0 * Math.asin(Math.sqrt(h));
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
