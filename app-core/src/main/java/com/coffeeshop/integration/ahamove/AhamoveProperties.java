package com.coffeeshop.integration.ahamove;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * app.delivery.ahamove.*: partner account (API token from Ahamove), service code and the shop as pick-up point.
 * {@code mode}: {@code staging} / {@code production} call the API; {@code simulated} (default) answers locally and a job
 * moves the trip along (assigning → accepted → in process → completed) through the same webhook handler.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.delivery.ahamove")
public class AhamoveProperties {
    private String mode = "simulated";
    private String baseUrl = "https://partner-apistg.ahamove.com/v3";
    private String token = "";
    /** SGN-BIKE (HCM), HAN-BIKE (Hà Nội)… */
    private String serviceId = "SGN-BIKE";
    /** shared secret the webhook URL carries (?token=…) */
    private String webhookToken = "coffeeholic-webhook";
    private String shopName = "Coffeeholic";
    private String shopPhone = "0900000000";
    private String shopAddress = "Coffeeholic, Quận 1, TP. Hồ Chí Minh";
    private Double shopLat = 10.7769;
    private Double shopLng = 106.7009;
    /** simulated: seconds before a driver accepts, and from pick-up to delivered */
    private int simulatedAcceptSeconds = 30;
    private int simulatedDeliverSeconds = 120;

    public boolean simulated() {
        return !"staging".equalsIgnoreCase(mode) && !"production".equalsIgnoreCase(mode);
    }
}
