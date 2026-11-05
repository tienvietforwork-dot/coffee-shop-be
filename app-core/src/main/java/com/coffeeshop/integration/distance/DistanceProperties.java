package com.coffeeshop.integration.distance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * app.delivery.distance.*: gợi ý địa chỉ, toạ độ và quãng đường quán → khách.
 * {@code mode}: {@code google} (Google Maps Platform: Places Autocomplete, Geocoding, Distance Matrix — cần API key),
 * {@code goong} (Goong.io, bản đồ Việt Nam — cần API key), {@code osm} (Photon + Nominatim + OSRM trên dữ liệu
 * OpenStreetMap, miễn phí), {@code auto} (mặc định: google → goong → osm theo key có sẵn), {@code simulated} (cho test).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.delivery.distance")
public class DistanceProperties {
    private String mode = "auto";
    private String googleApiKey = "";
    private String goongApiKey = "";
    private String goongBaseUrl = "https://rsapi.goong.io";
    private String photonUrl = "https://photon.komoot.io";
    private String nominatimUrl = "https://nominatim.openstreetmap.org";
    private String osrmUrl = "https://router.project-osrm.org";
    /** Nominatim / Photon require an identifying User-Agent */
    private String userAgent = "Coffeeholic/1.0 (coffee-shop-be)";

    public String effectiveMode() {
        if (!"auto".equalsIgnoreCase(mode)) return mode.toLowerCase();
        if (googleApiKey != null && !googleApiKey.isBlank()) return "google";
        if (goongApiKey != null && !goongApiKey.isBlank()) return "goong";
        return "osm";
    }
}
