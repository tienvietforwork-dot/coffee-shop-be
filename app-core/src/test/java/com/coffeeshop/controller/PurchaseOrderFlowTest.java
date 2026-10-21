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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Supplier → purchase order → partial and final receipts → lots in stock, through the HTTP API on H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"suppliers", "purchase-orders", "stock", "materials"})
class PurchaseOrderFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaterialRepository materialRepository;

    Material milk;
    Long box;

    @BeforeEach
    void setUp() {
        milk = Material.builder().name("Sữa tươi " + UUID.randomUUID()).unit("ml").stockQuantity(BigDecimal.ZERO)
                .minStock(new BigDecimal("2000")).build();
        milk.getPackages().add(MaterialPackage.builder().material(milk).name("Hộp 1 L").size(new BigDecimal("1000")).build());
        milk = materialRepository.save(milk);
        box = milk.getPackages().get(0).getId();
    }

    private JsonNode call(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String body(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    private long createSupplier() throws Exception {
        var res = call(mvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(body(java.util.Map.of(
                "name", "NCC " + UUID.randomUUID(), "phone", "0912000009",
                "materials", java.util.List.of(java.util.Map.of("materialId", milk.getId(), "packageId", box, "unitPrice", 35)))))).andExpect(status().isCreated()));
        assertThat(res.at("/materials/0/packageName").asText()).isEqualTo("Hộp 1 L");
        return res.get("id").asLong();
    }

    @Test
    void orderSendReceiveInTwoDeliveriesThenCompleted() throws Exception {
        long supplierId = createSupplier();

        // draft: 10 boxes × 1 L at 35đ/ml
        var po = call(mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body(java.util.Map.of(
                "supplierId", supplierId, "expectedDate", LocalDate.now().plusDays(2).toString(),
                "items", java.util.List.of(java.util.Map.of("materialId", milk.getId(), "packageId", box, "packageCount", 10, "unitPrice", 35))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].quantity").value(10000))
                .andExpect(jsonPath("$.totalAmount").value(350000)));
        long id = po.get("id").asLong();
        long itemId = po.at("/items/0/id").asLong();
        assertThat(po.get("code").asText()).startsWith("DH");

        // a draft cannot be received
        mvc.perform(post("/api/purchase-orders/" + id + "/receipts").contentType(MediaType.APPLICATION_JSON)
                .content(body(java.util.Map.of("lines", java.util.List.of(java.util.Map.of("itemId", itemId, "receivedCount", 1))))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/purchase-orders/" + id + "/send")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ORDERED"));
        // once sent it can no longer be edited or deleted
        mvc.perform(delete("/api/purchase-orders/" + id)).andExpect(status().isBadRequest());

        // first delivery: 4 boxes
        mvc.perform(post("/api/purchase-orders/" + id + "/receipts").contentType(MediaType.APPLICATION_JSON).content(body(java.util.Map.of(
                        "note", "Giao đợt 1",
                        "lines", java.util.List.of(java.util.Map.of("itemId", itemId, "receivedCount", 4, "expiryDate", LocalDate.now().plusDays(10).toString()))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(4000))
                .andExpect(jsonPath("$.lines[0].packageName").value("Hộp 1 L"))
                .andExpect(jsonPath("$.totalAmount").value(140000));
        mvc.perform(get("/api/purchase-orders/" + id)).andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(4000))
                .andExpect(jsonPath("$.receipts.length()").value(1));
        // partially received: cannot be cancelled any more
        mvc.perform(post("/api/purchase-orders/" + id + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest());
        // more than what is still due is refused
        mvc.perform(post("/api/purchase-orders/" + id + "/receipts").contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of("lines", java.util.List.of(java.util.Map.of("itemId", itemId, "receivedCount", 7))))))
                .andExpect(status().isBadRequest());

        // second delivery: the remaining 6 boxes → completed
        mvc.perform(post("/api/purchase-orders/" + id + "/receipts").contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of("lines", java.util.List.of(java.util.Map.of("itemId", itemId, "receivedCount", 6))))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/purchase-orders/" + id)).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.receipts.length()").value(2));

        assertThat(materialRepository.findById(milk.getId()).orElseThrow().getStockQuantity()).isEqualByComparingTo("10000");
        mvc.perform(get("/api/materials/" + milk.getId() + "/batches"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].receiptCode").exists())
                .andExpect(jsonPath("$[0].supplierName").exists());
        mvc.perform(get("/api/goods-receipts").param("purchaseOrderId", String.valueOf(id))).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void partiallyReceivedOrderIsClosedAndAnUnreceivedOneCancelled() throws Exception {
        long supplierId = createSupplier();
        var order = java.util.Map.of("supplierId", supplierId,
                "items", java.util.List.of(java.util.Map.of("materialId", milk.getId(), "quantity", 3000)));
        long a = call(mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body(order)))).get("id").asLong();
        long b = call(mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body(order)))).get("id").asLong();
        long c = call(mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body(order)))).get("id").asLong();

        // draft deleted outright
        mvc.perform(delete("/api/purchase-orders/" + c)).andExpect(status().isNoContent());
        mvc.perform(get("/api/purchase-orders/" + c)).andExpect(status().isNotFound());

        mvc.perform(post("/api/purchase-orders/" + a + "/send")).andExpect(status().isOk());
        mvc.perform(post("/api/purchase-orders/" + b + "/send")).andExpect(status().isOk());

        long itemA = call(mvc.perform(get("/api/purchase-orders/" + a))).at("/items/0/id").asLong();
        mvc.perform(post("/api/purchase-orders/" + a + "/receipts").contentType(MediaType.APPLICATION_JSON)
                .content(body(java.util.Map.of("lines", java.util.List.of(java.util.Map.of("itemId", itemA, "receivedCount", 1000)))))).andExpect(status().isCreated());
        mvc.perform(post("/api/purchase-orders/" + a + "/close").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"NCC hết hàng\"}"))
                .andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.closeReason").value("NCC hết hàng"));

        mvc.perform(post("/api/purchase-orders/" + b + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Đổi nhà cung cấp\"}"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void looseImportNeedsAnActiveSupplier() throws Exception {
        long supplierId = createSupplier();
        String line = "[{\"materialId\":" + milk.getId() + ",\"receivedCount\":500}]";
        mvc.perform(post("/api/goods-receipts").contentType(MediaType.APPLICATION_JSON).content("{\"lines\":" + line + "}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/goods-receipts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":" + supplierId + ",\"lines\":[{\"materialId\":" + milk.getId() + ",\"packageId\":" + box + ",\"receivedCount\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(2000))
                .andExpect(jsonPath("$.code").exists());
        mvc.perform(get("/api/goods-receipts").param("supplierId", String.valueOf(supplierId)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purchaseOrderId").doesNotExist());

        // ngừng hợp tác: no new orders or imports with it
        mvc.perform(delete("/api/suppliers/" + supplierId)).andExpect(status().isNoContent());
        mvc.perform(post("/api/goods-receipts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":" + supplierId + ",\"lines\":" + line + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"stock"})
    void staffWithStockCanReadButNotManageOrders() throws Exception {
        mvc.perform(get("/api/purchase-orders")).andExpect(status().isOk());
        mvc.perform(get("/api/suppliers")).andExpect(status().isOk());
        // (bodies are validated before the permission check, so the write calls tried here carry none)
        mvc.perform(post("/api/purchase-orders/1/send")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/purchase-orders/1")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/suppliers/1")).andExpect(status().isForbidden());
    }
}
