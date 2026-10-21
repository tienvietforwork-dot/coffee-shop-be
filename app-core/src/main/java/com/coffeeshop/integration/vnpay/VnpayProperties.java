package com.coffeeshop.integration.vnpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * app.payment.vnpay.*: merchant settings from VNPAY (TmnCode, HashSecret). {@code mode}: {@code sandbox} calls
 * sandbox.vnpayment.vn with real keys; {@code simulated} (default, no keys needed) signs the same way and sends the
 * customer to a built-in test page that answers like VNPAY (IPN + return), so the whole flow runs offline.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VnpayProperties {
    private String mode = "simulated";
    private String tmnCode = "COFFEEHL";
    private String hashSecret = "SIMULATED-HASH-SECRET-0123456789";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    /** where VNPAY sends the customer back (FE page that shows the result) */
    private String returnUrl = "http://localhost:5173/payment/result";
    /** public base URL of this API, for the simulated payment page */
    private String publicApiBase = "http://localhost:8080";
    private int expireMinutes = 15;

    public boolean simulated() {
        return !"sandbox".equalsIgnoreCase(mode) && !"production".equalsIgnoreCase(mode);
    }
}
