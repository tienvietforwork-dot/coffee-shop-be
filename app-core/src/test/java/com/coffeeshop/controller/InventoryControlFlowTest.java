package com.coffeeshop.controller;

import com.coffeeshop.entity.*;
import com.coffeeshop.repository.*;
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
import org.springframework.test.web.servlet.RequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nhập 2 lô có giá → phiếu kiểm kê (thiếu) → cân bằng kho → báo cáo xuất – nhập – tồn → giá vốn món. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"suppliers", "stock", "stocktakes", "inventory-report", "coffee-costs", "menu.edit"})
class InventoryControlFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaterialRepository materialRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired RecipeRepository recipeRepository;

    Material beans;
    long supplierId;

    @BeforeEach
    void setUp() throws Exception {
        beans = materialRepository.save(Material.builder().name("Hạt cà phê " + UUID.randomUUID()).unit("g")
                .stockQuantity(BigDecimal.ZERO).minStock(BigDecimal.ZERO).build());
        supplierId = call(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "NCC " + UUID.randomUUID())))).get("id").asLong();
        // two lots: 1000 g at 300đ/g and 1000 g at 400đ/g
        for (int price : new int[]{300, 400}) {
            mvc.perform(post("/api/goods-receipts").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                    "supplierId", supplierId, "lines", List.of(Map.of("materialId", beans.getId(), "receivedCount", 1000, "unitPrice", price))))))
                    .andExpect(status().isCreated());
        }
    }

    private JsonNode call(RequestBuilder r) throws Exception {
        return json.readTree(mvc.perform(r).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder body(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, Object o) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(o));
    }

    @Test
    void stocktakeBalancesTheShortageAndTheReportShowsIt() throws Exception {
        JsonNode st = call(body(post("/api/stocktakes"), Map.of("materialIds", List.of(beans.getId()), "members", "Lan, Minh")));
        assertThat(st.get("items").size()).isEqualTo(2);
        long first = st.at("/items/0/id").asLong(), second = st.at("/items/1/id").asLong();
        // cannot balance before everything is counted
        mvc.perform(body(post("/api/stocktakes/" + st.get("id").asLong() + "/balance"), Map.of()))
                .andExpect(status().isBadRequest());
        st = call(body(put("/api/stocktakes/" + st.get("id").asLong()), Map.of("counts", List.of(
                Map.of("itemId", first, "countedQuantity", 950, "reason", "Hao hụt khi xay"),
                Map.of("itemId", second, "countedQuantity", 1000)))));
        assertThat(st.get("shortageValue").decimalValue()).isEqualByComparingTo("15000");
        st = call(body(post("/api/stocktakes/" + st.get("id").asLong() + "/balance"), Map.of("resolution", "Tính vào chi phí")));
        assertThat(st.get("status").asText()).isEqualTo("BALANCED");
        assertThat(materialRepository.findById(beans.getId()).orElseThrow().getStockQuantity()).isEqualByComparingTo("1950");

        String today = LocalDate.now().toString();
        JsonNode report = call(get("/api/reports/inventory").param("from", today).param("to", today));
        JsonNode row = null;
        for (JsonNode r : report.get("rows")) if (r.get("materialId").asLong() == beans.getId()) row = r;
        assertThat(row).isNotNull();
        assertThat(row.get("openingQuantity").decimalValue()).isEqualByComparingTo("0");
        assertThat(row.get("inPurchase").decimalValue()).isEqualByComparingTo("2000");
        assertThat(row.get("outOther").decimalValue()).isEqualByComparingTo("50");
        assertThat(row.get("closingQuantity").decimalValue()).isEqualByComparingTo("1950");
        assertThat(row.get("closingValue").decimalValue()).isEqualByComparingTo("685000");
        mvc.perform(get("/api/reports/inventory.xlsx").param("from", today).param("to", today)).andExpect(status().isOk());

        // the evening job closes the day
        mvc.perform(post("/api/reports/inventory/close-day")).andExpect(status().isOk());
        JsonNode daily = call(get("/api/reports/inventory/daily").param("from", today).param("to", today)
                .param("materialId", String.valueOf(beans.getId())));
        assertThat(daily.get(0).get("quantity").decimalValue()).isEqualByComparingTo("1950");
    }

    @Test
    void coffeeCostFollowsTheLotsInStock() throws Exception {
        Category cat = categoryRepository.save(Category.builder().name("Cà phê " + UUID.randomUUID()).build());
        Coffee coffee = coffeeRepository.save(Coffee.builder().category(cat).name("Đen đá " + UUID.randomUUID())
                .price(new BigDecimal("29000")).build());
        Recipe recipe = Recipe.builder().coffee(coffee).build();
        recipe.getMaterials().add(RecipeMaterial.builder().recipe(recipe).material(beans).quantity(new BigDecimal("20")).build());
        recipeRepository.save(recipe);

        JsonNode all = call(get("/api/coffee-costs"));
        JsonNode mine = null;
        for (JsonNode c : all) if (c.get("coffeeId").asLong() == coffee.getId()) mine = c;
        assertThat(mine).isNotNull();
        // average of the lots in stock: (1000×300 + 1000×400) / 2000 = 350đ/g × 20 g = 7.000đ → 24,1% of 29.000đ
        assertThat(mine.get("unitCost").decimalValue()).isEqualByComparingTo("7000");
        assertThat(mine.get("foodCostPercent").decimalValue()).isEqualByComparingTo("24.1");
        assertThat(mine.get("complete").asBoolean()).isTrue();

        JsonNode priced = call(body(put("/api/coffee-costs/" + coffee.getId() + "/price"), Map.of("price", 35000)));
        assertThat(priced.get("grossMargin").decimalValue()).isEqualByComparingTo("28000");
        assertThat(call(get("/api/coffee-costs/" + coffee.getId() + "/history")).size()).isEqualTo(1);
    }
}
