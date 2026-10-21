package com.coffeeshop.integration.esms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * eSMS multi-channel API (developers.esms.vn): Zalo ZNS first, SMS brandname (SmsType 2 = chăm sóc khách hàng) when ZNS
 * fails. CodeResult 100 only means eSMS accepted the request; delivery comes on the callback.
 * app.notify.esms.mode: {@code simulated} (default) accepts locally; {@code sandbox} / {@code production} call eSMS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsmsGateway {

    private final Props props;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    @Data
    @Component
    @ConfigurationProperties(prefix = "app.notify.esms")
    public static class Props {
        private String mode = "simulated";
        private String url = "https://rest.esms.vn/MainService.svc/json/MultiChannelMessage/";
        private String apiKey = "";
        private String secretKey = "";
        private String brandname = "COFFEEHOLIC";
        /** Zalo Official Account id */
        private String oaId = "";
        private String callbackUrl = "";
        private String callbackToken = "coffeeholic-esms";

        boolean simulated() {
            return !"sandbox".equalsIgnoreCase(mode) && !"production".equalsIgnoreCase(mode);
        }
    }

    public record Result(boolean accepted, String channel, String providerRef, String error) {
    }

    public Props props() {
        return props;
    }

    /** {@code znsTemplateId} null: SMS only */
    public Result send(String phone, String znsTemplateId, Map<String, String> params, String smsContent, boolean smsFallback,
                       String requestId) {
        boolean zns = znsTemplateId != null && !znsTemplateId.isBlank();
        if (props.simulated()) {
            return new Result(true, zns ? "ZNS" : "SMS", "SIM-" + requestId, null);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ApiKey", props.getApiKey());
        body.put("SecretKey", props.getSecretKey());
        body.put("Phone", phone);
        body.put("Channels", zns ? (smsFallback ? List.of("zalo", "sms") : List.of("zalo")) : List.of("sms"));
        if (zns) {
            body.put("Data", List.of(Map.of("TempID", znsTemplateId, "Params", params.values().stream().toList(), "OAID", props.getOaId(),
                    "campaignid", "Don hang", "RequestId", requestId, "Sandbox", "sandbox".equalsIgnoreCase(props.getMode()) ? "1" : "0")));
        }
        body.put("Content", smsContent);
        body.put("Brandname", props.getBrandname());
        body.put("SmsType", "2");
        body.put("RequestId", requestId);
        if (!props.getCallbackUrl().isBlank()) body.put("CallbackUrl", props.getCallbackUrl() + "?token=" + props.getCallbackToken());
        if ("sandbox".equalsIgnoreCase(props.getMode())) body.put("Sandbox", "1");
        try {
            String raw = http.post().uri(props.getUrl()).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            JsonNode r = json.readTree(raw);
            String code = r.path("CodeResult").asText();
            return "100".equals(code)
                    ? new Result(true, zns ? "ZNS" : "SMS", r.path("SMSID").asText(null), null)
                    : new Result(false, null, null, "eSMS " + code + ": " + r.path("ErrorMessage").asText(""));
        } catch (Exception e) {
            log.warn("eSMS failed: {}", e.getMessage());
            return new Result(false, null, null, e.getMessage());
        }
    }
}
