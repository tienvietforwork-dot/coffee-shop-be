package com.coffeeshop.integration.einvoice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * app.einvoice.*: seller and invoice settings registered with the tax office, and the provider account.
 * {@code vatRate} null → hóa đơn bán hàng (mẫu 2, hộ kinh doanh / phương pháp trực tiếp); a rate → hóa đơn GTGT (mẫu 1).
 * {@code mode}: {@code simulated} (default) numbers and "issues" locally; {@code misa} calls MISA meInvoice
 * (its API documentation is given with the contract; the call is isolated in {@link EinvoiceProvider}).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.einvoice")
public class EinvoiceProperties {
    private String mode = "simulated";
    private String sellerName = "HỘ KINH DOANH COFFEEHOLIC";
    private String sellerTaxCode = "0312345678";
    private String sellerAddress = "Quận 1, TP. Hồ Chí Minh";
    /** 1C26MCH: 1 = GTGT (2 = bán hàng), C = có mã CQT, 26 = năm, M = máy tính tiền, CH = tự đặt */
    private String series = "2C26MCH";
    private BigDecimal vatRate;
    private String lookupUrl = "https://www.meinvoice.vn/tra-cuu";
    private String apiUrl = "https://api.meinvoice.vn/api/integration";
    private String appId = "";
    private String username = "";
    private String password = "";
    private int maxAttempts = 5;

    public boolean simulated() {
        return !"misa".equalsIgnoreCase(mode);
    }

    public String templateCode() {
        return vatRate == null ? "2" : "1";
    }
}
