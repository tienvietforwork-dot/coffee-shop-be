package com.coffeeshop.integration.einvoice;

import com.coffeeshop.entity.Invoice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phát hành hóa đơn qua nhà cung cấp (MISA meInvoice): sends seller, buyer (if any), every line, totals; gets back
 * số hóa đơn, mã của CQT, mã tra cứu. Simulated mode numbers the invoice itself and makes the codes up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EinvoiceProvider {

    private final EinvoiceProperties props;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    public record Issued(String invoiceNo, String taxAuthorityCode, String lookupCode, String providerId) {
    }

    public EinvoiceProperties props() {
        return props;
    }

    /** {@code number}: next number of the series (used by the simulation; MISA numbers it itself) */
    public Issued issue(Invoice inv, int number) {
        if (props.simulated()) {
            String no = String.format("%08d", number);
            String cqt = "M" + inv.getSeries().substring(0, 1) + "-" + inv.getCreatedAt().getYear() % 100 + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase() + "-" + no;
            return new Issued(no, cqt, UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                    "SIM-" + inv.getId());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("RefID", "CH-" + inv.getId());
        body.put("InvSeries", inv.getSeries());
        body.put("InvTemplate", inv.getTemplateCode());
        body.put("IsInvoiceCalculatingMachine", true);
        body.put("SellerTaxCode", props.getSellerTaxCode());
        body.put("BuyerLegalName", inv.getBuyerCompany());
        body.put("BuyerFullName", inv.getBuyerName() == null && inv.getBuyerCompany() == null ? "Người mua không lấy hóa đơn" : inv.getBuyerName());
        body.put("BuyerTaxCode", inv.getBuyerTaxCode());
        body.put("BuyerAddress", inv.getBuyerAddress());
        body.put("BuyerEmail", inv.getBuyerEmail());
        body.put("BuyerPhoneNumber", inv.getBuyerPhone());
        body.put("BuyerIDNumber", inv.getBuyerIdNumber());
        body.put("PaymentMethodName", inv.getPaymentMethod());
        body.put("TotalAmountWithoutVAT", inv.getAmountBeforeTax());
        body.put("TotalVATAmount", inv.getVatAmount());
        body.put("TotalAmount", inv.getTotalAmount());
        if (inv.getRelated() != null) {
            body.put("OrgInvoiceType", "REPLACEMENT".equals(inv.getKind()) ? 3 : 2);
            body.put("OrgInvNo", inv.getRelated().getInvoiceNo());
            body.put("OrgInvSeries", inv.getRelated().getSeries());
            body.put("OrgInvDate", inv.getRelated().getIssuedAt() == null ? null : inv.getRelated().getIssuedAt().toLocalDate().toString());
            body.put("ReferenceReason", inv.getReason());
        }
        body.put("OriginalInvoiceDetail", inv.getLines().stream().map(l -> Map.of(
                "SortOrder", l.getLineNo(), "ItemName", l.getName(), "UnitName", l.getUnit(), "Quantity", l.getQuantity(),
                "UnitPrice", l.getUnitPrice(), "AmountOC", l.getAmount(),
                "VATRateName", props.getVatRate() == null ? "KCT" : props.getVatRate().stripTrailingZeros().toPlainString() + "%")).toList());
        try {
            String raw = http.post().uri(props.getApiUrl() + "/invoice/publishing")
                    .header("appid", props.getAppId()).header("username", props.getUsername()).header("password", props.getPassword())
                    .contentType(MediaType.APPLICATION_JSON).body(List.of(body)).retrieve().body(String.class);
            JsonNode r = json.readTree(raw);
            JsonNode data = r.path("Data");
            if (!r.path("Success").asBoolean(false)) throw new IllegalStateException(r.path("ErrorCode").asText("MISA lỗi"));
            JsonNode first = data.isArray() ? data.get(0) : data;
            return new Issued(first.path("InvNo").asText(), first.path("InvCode").asText(null),
                    first.path("TransactionID").asText(null), first.path("InvID").asText(null));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không gọi được nhà cung cấp hóa đơn: " + e.getMessage(), e);
        }
    }
}
