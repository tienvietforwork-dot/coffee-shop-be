package com.coffeeshop.controller;

import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialPackage;
import com.coffeeshop.repository.MaterialRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phiếu nhập (thực nhận / trả lại, VAT, trả ngay) → phiếu trả hàng → phiếu chi → công nợ; hủy phiếu nhập. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"suppliers", "purchase-orders", "stock", "materials", "supplier-returns", "supplier-debts"})
class SupplierDebtFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaterialRepository materialRepository;

    Material milk;
    Long box;
    long supplierId;

    @BeforeEach
    void setUp() throws Exception {
        milk = Material.builder().name("Sữa " + UUID.randomUUID()).unit("ml").stockQuantity(BigDecimal.ZERO)
                .minStock(BigDecimal.ZERO).build();
        milk.getPackages().add(MaterialPackage.builder().material(milk).name("Hộp 1 L").size(new BigDecimal("1000")).build());
        milk = materialRepository.save(milk);
        box = milk.getPackages().get(0).getId();
        supplierId = call(post("/api/suppliers", Map.of("name", "NCC " + UUID.randomUUID(), "paymentTermDays", 15)))
                .get("id").asLong();
    }

    private JsonNode call(org.springframework.test.web.servlet.RequestBuilder r) throws Exception {
        return json.readTree(mvc.perform(r).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url, Object body) throws Exception {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private ResultActions perform(org.springframework.test.web.servlet.RequestBuilder r) throws Exception {
        return mvc.perform(r);
    }

    /** 10 hộp on the delivery note, 9 accepted, 1 dented and refused; 30.000đ / hộp (30đ/ml), VAT 8%, 50.000đ paid. */
    private JsonNode receive() throws Exception {
        return call(post("/api/goods-receipts", Map.of(
                "supplierId", supplierId, "delivererName", "Anh Tài", "invoiceSeries", "1C26TAA", "invoiceNo", "123",
                "invoiceDate", LocalDate.now().toString(), "vatRate", 8, "shippingCost", 20000,
                "paidAmount", 50000, "paymentMethod", "CASH",
                "lines", List.of(Map.of("materialId", milk.getId(), "packageId", box, "documentCount", 10,
                        "receivedCount", 9, "rejectedCount", 1, "rejectReason", "Hộp móp", "unitPrice", 30,
                        "expiryDate", LocalDate.now().plusDays(7).toString(), "temperature", 4)))));
    }

    @Test
    void receiptReturnPaymentAndDebt() throws Exception {
        JsonNode receipt = receive();
        // 9 × 1000 ml × 30đ = 270.000; VAT 8% = 21.600; + 20.000 vận chuyển = 311.600
        assertThat(receipt.get("goodsAmount").decimalValue()).isEqualByComparingTo("270000");
        assertThat(receipt.get("vatAmount").decimalValue()).isEqualByComparingTo("21600");
        assertThat(receipt.get("totalAmount").decimalValue()).isEqualByComparingTo("311600");
        assertThat(receipt.get("paidAmount").decimalValue()).isEqualByComparingTo("50000");
        assertThat(receipt.get("remainingAmount").decimalValue()).isEqualByComparingTo("261600");
        assertThat(receipt.get("dueDate").asText()).isEqualTo(LocalDate.now().plusDays(15).toString());
        assertThat(receipt.at("/lines/0/rejectedQuantity").decimalValue()).isEqualByComparingTo("1000");
        assertThat(receipt.at("/lines/0/documentQuantity").decimalValue()).isEqualByComparingTo("10000");
        assertThat(materialRepository.findById(milk.getId()).orElseThrow().getStockQuantity()).isEqualByComparingTo("9000");

        // trả 2 hộp cận date, trừ công nợ: 2 × 1000 × 30 = 60.000 + 8% = 64.800
        long lineId = receipt.at("/lines/0/id").asLong();
        JsonNode ret = call(post("/api/supplier-returns", Map.of("goodsReceiptId", receipt.get("id").asLong(),
                "reason", "Cận date", "settlement", "DEBT", "complete", true,
                "items", List.of(Map.of("receiptItemId", lineId, "count", 2)))));
        assertThat(ret.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(ret.get("totalAmount").decimalValue()).isEqualByComparingTo("64800");
        assertThat(materialRepository.findById(milk.getId()).orElseThrow().getStockQuantity()).isEqualByComparingTo("7000");
        // cannot return more than what is left of the line
        perform(post("/api/supplier-returns", Map.of("goodsReceiptId", receipt.get("id").asLong(), "reason", "x",
                "settlement", "DEBT", "items", List.of(Map.of("receiptItemId", lineId, "count", 8)))))
                .andExpect(status().isBadRequest());

        // trả nợ 100.000 chuyển khoản
        perform(post("/api/supplier-payments", Map.of("supplierId", supplierId, "amount", 100000,
                "method", "BANK_TRANSFER", "reference", "FT2610")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").exists());

        JsonNode debt = call(get("/api/supplier-debts"));
        JsonNode mine = null;
        for (JsonNode d : debt) if (d.get("supplierId").asLong() == supplierId) mine = d;
        assertThat(mine).isNotNull();
        // 311.600 − 64.800 − 50.000 − 100.000 = 96.800
        assertThat(mine.get("balance").decimalValue()).isEqualByComparingTo("96800");
        assertThat(mine.get("openReceipts").asInt()).isEqualTo(1);
        // no paying more than the receipt / the supplier is still owed
        perform(post("/api/supplier-payments", Map.of("supplierId", supplierId, "amount", 96801, "method", "CASH",
                "goodsReceiptId", receipt.get("id").asLong()))).andExpect(status().isBadRequest());
        perform(post("/api/supplier-payments", Map.of("supplierId", supplierId, "amount", 5000000, "method", "CASH")))
                .andExpect(status().isBadRequest());

        JsonNode ledger = call(get("/api/supplier-debts/" + supplierId + "/ledger"));
        assertThat(ledger.get("rows").size()).isEqualTo(4);
        assertThat(ledger.get("closingBalance").decimalValue()).isEqualByComparingTo("96800");
        perform(get("/api/supplier-debts/" + supplierId + "/ledger.xlsx")).andExpect(status().isOk());

        // a receipt with a return cannot be cancelled any more
        perform(post("/api/goods-receipts/" + receipt.get("id").asLong() + "/cancel", Map.of("reason", "nhầm")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void untouchedReceiptIsCancelledAndTheOrderReopens() throws Exception {
        JsonNode po = call(post("/api/purchase-orders", Map.of("supplierId", supplierId, "vatRate", 10,
                "items", List.of(Map.of("materialId", milk.getId(), "packageId", box, "packageCount", 5, "unitPrice", 30)))));
        long id = po.get("id").asLong();
        assertThat(po.get("paymentTermDays").asInt()).isEqualTo(15);
        assertThat(po.get("totalAmount").decimalValue()).isEqualByComparingTo("165000");
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/purchase-orders/" + id + "/send"))
                .andExpect(status().isOk());
        JsonNode receipt = call(post("/api/purchase-orders/" + id + "/receipts", Map.of("lines",
                List.of(Map.of("itemId", po.at("/items/0/id").asLong(), "receivedCount", 5)))));
        assertThat(receipt.get("vatRate").decimalValue()).isEqualByComparingTo("10");
        perform(get("/api/purchase-orders/" + id)).andExpect(jsonPath("$.status").value("COMPLETED"));

        perform(post("/api/goods-receipts/" + receipt.get("id").asLong() + "/cancel", Map.of("reason", "Nhập nhầm nhà cung cấp")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.totalAmount").value(0));
        assertThat(materialRepository.findById(milk.getId()).orElseThrow().getStockQuantity()).isEqualByComparingTo("0");
        perform(get("/api/purchase-orders/" + id)).andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(0));
    }

    @Test
    void refusedGoodsNeedAReasonAndCannotExceedTheDeliveryNote() throws Exception {
        perform(post("/api/goods-receipts", Map.of("supplierId", supplierId, "lines", List.of(Map.of(
                "materialId", milk.getId(), "packageId", box, "receivedCount", 3, "rejectedCount", 1)))))
                .andExpect(status().isBadRequest());
        perform(post("/api/goods-receipts", Map.of("supplierId", supplierId, "lines", List.of(Map.of(
                "materialId", milk.getId(), "packageId", box, "documentCount", 3, "receivedCount", 3, "rejectedCount", 1,
                "rejectReason", "Hỏng")))))
                .andExpect(status().isBadRequest());
        // everything refused: the line is kept, no lot is made
        JsonNode r = call(post("/api/goods-receipts", Map.of("supplierId", supplierId, "lines", List.of(Map.of(
                "materialId", milk.getId(), "packageId", box, "receivedCount", 0, "rejectedCount", 2,
                "rejectReason", "Sữa không đủ lạnh", "temperature", 12)))));
        assertThat(r.at("/lines/0/batchId").isMissingNode()).isTrue(); // null fields are left out
        assertThat(r.get("totalAmount").decimalValue()).isEqualByComparingTo("0");
    }
}
