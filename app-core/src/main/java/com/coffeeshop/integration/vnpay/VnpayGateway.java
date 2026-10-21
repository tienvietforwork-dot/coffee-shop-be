package com.coffeeshop.integration.vnpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * VNPAY payment gateway, API 2.1.0 (sandbox.vnpayment.vn/apis/docs):
 * <ul>
 *   <li>pay URL: vnp_* parameters sorted by name, values URL-encoded, signed with HMAC-SHA512(HashSecret) → vnp_SecureHash;</li>
 *   <li>return / IPN: the same signature is checked over every vnp_* parameter except the hash;</li>
 *   <li>querydr / refund: JSON to merchant_webapi, checksum over the fields joined with "|".</li>
 * </ul>
 * Amounts go ×100 (no decimals); dates are yyyyMMddHHmmss in GMT+7.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VnpayGateway {

    public static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    /** answer of querydr / refund */
    public record Answer(String responseCode, String transactionStatus, String transactionNo, String bankCode, String raw) {
        public boolean ok() {
            return "00".equals(responseCode);
        }
    }

    public boolean simulated() {
        return props.simulated();
    }

    public int expireMinutes() {
        return props.getExpireMinutes();
    }

    public static String now() {
        return LocalDateTime.now(VN).format(VNP_DATE);
    }

    /** the URL the customer is sent to; {@code bankCode}: VNPAYQR, VNBANK, INTCARD or null (customer picks) */
    public String payUrl(String txnRef, BigDecimal amount, String orderInfo, String bankCode, String ip, String createDate,
                         String expireDate) {
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", props.getTmnCode());
        p.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", orderInfo);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", props.getReturnUrl());
        p.put("vnp_IpAddr", ip == null || ip.isBlank() ? "127.0.0.1" : ip);
        p.put("vnp_CreateDate", createDate);
        p.put("vnp_ExpireDate", expireDate);
        if (bankCode != null && !bankCode.isBlank()) p.put("vnp_BankCode", bankCode);
        String query = query(p);
        String base = props.simulated() ? props.getPublicApiBase() + "/api/public/payments/sandbox" : props.getPayUrl();
        return base + "?" + query + "&vnp_SecureHash=" + hmac(hashData(p));
    }

    /** true when vnp_SecureHash matches the other vnp_* parameters */
    public boolean verify(Map<String, String> params) {
        String given = params.get("vnp_SecureHash");
        if (given == null) return false;
        Map<String, String> fields = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType") && v != null && !v.isEmpty()) {
                fields.put(k, v);
            }
        });
        return hmac(hashData(fields)).equalsIgnoreCase(given);
    }

    /** signed callback parameters as VNPAY sends them (used by the simulated page) */
    public Map<String, String> signedResult(Map<String, String> request, String responseCode, String transactionNo) {
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_TmnCode", request.get("vnp_TmnCode"));
        p.put("vnp_Amount", request.get("vnp_Amount"));
        p.put("vnp_BankCode", request.getOrDefault("vnp_BankCode", "NCB"));
        p.put("vnp_OrderInfo", request.get("vnp_OrderInfo"));
        p.put("vnp_PayDate", now());
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionNo", transactionNo);
        p.put("vnp_TransactionStatus", "00".equals(responseCode) ? "00" : "02");
        p.put("vnp_TxnRef", request.get("vnp_TxnRef"));
        p.put("vnp_CardType", "QRCODE");
        Map<String, String> out = new LinkedHashMap<>(p);
        out.put("vnp_SecureHash", hmac(hashData(p)));
        return out;
    }

    public String returnUrlWith(Map<String, String> params) {
        return props.getReturnUrl() + "?" + query(params);
    }

    /** querydr: the state of a transaction at VNPAY */
    public Answer query(String txnRef, String transactionDate, String ip) {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String createDate = now();
        String orderInfo = "Truy van GD " + txnRef;
        String data = String.join("|", requestId, "2.1.0", "querydr", props.getTmnCode(), txnRef, transactionDate,
                createDate, ip, orderInfo);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", "2.1.0");
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", hmac(data));
        return call(body);
    }

    /** refund: transactionType 02 = toàn phần, 03 = một phần */
    public Answer refund(String txnRef, BigDecimal amount, boolean full, String transactionNo, String transactionDate,
                         String createBy, String ip) {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String createDate = now();
        String type = full ? "02" : "03";
        String vnpAmount = amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        String orderInfo = "Hoan tien GD " + txnRef;
        String data = String.join("|", requestId, "2.1.0", "refund", props.getTmnCode(), type, txnRef, vnpAmount,
                transactionNo == null ? "" : transactionNo, transactionDate, createBy, createDate, ip, orderInfo);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", "2.1.0");
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TransactionType", type);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_Amount", vnpAmount);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", transactionNo == null ? "" : transactionNo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", createBy);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", hmac(data));
        return call(body);
    }

    private Answer call(Map<String, Object> body) {
        try {
            String raw = http.post().uri(props.getApiUrl()).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(String.class);
            var node = json.readTree(raw);
            return new Answer(node.path("vnp_ResponseCode").asText(null), node.path("vnp_TransactionStatus").asText(null),
                    node.path("vnp_TransactionNo").asText(null), node.path("vnp_BankCode").asText(null), raw);
        } catch (Exception e) {
            log.warn("VNPAY {} failed: {}", body.get("vnp_Command"), e.getMessage());
            return new Answer("99", null, null, null, String.valueOf(e.getMessage()));
        }
    }

    private static String hashData(Map<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (v == null || v.isEmpty()) return;
            if (!sb.isEmpty()) sb.append('&');
            sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
        });
        return sb.toString();
    }

    private static String query(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (v == null || v.isEmpty()) return;
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
        });
        return sb.toString();
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(props.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA512 unavailable", e);
        }
    }
}
