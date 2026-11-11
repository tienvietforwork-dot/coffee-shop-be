package com.coffeeshop.controller;

import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.*;
import com.coffeeshop.integration.distance.DistanceGateway;
import com.coffeeshop.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phí ship theo khu vực (tỉnh, phường phí riêng / không giao, miễn phí từ, đơn tối thiểu); quán tự giao: bắt đầu giao → đã giao. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"delivery-zones", "pos", "shipments", "orders"})
class DeliveryFlowTest {

    static final String BA_DINH = "12 Điện Biên Phủ, Phường Ba Đình, Thành phố Hà Nội";
    static final String SOC_SON = "1 Núi Đôi, Xã Sóc Sơn, Thành phố Hà Nội";
    static final String HOAN_KIEM = "5 Tràng Tiền, Phường Hoàn Kiếm, TP. Hà Nội";
    static final String SAI_GON = "12 Lê Lợi, Phường Sài Gòn, Thành phố Hồ Chí Minh";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerAddressRepository addressRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired DeliveryProvinceRepository provinceRepository;
    @MockBean DistanceGateway distance;

    Coffee latte;

    @BeforeEach
    void setUp() throws Exception {
        when(distance.provider()).thenReturn("test");
        when(distance.geocode(any(), any())).thenReturn(new DistanceGateway.Point(21.03, 105.85));
        when(distance.route(any(), any())).thenReturn(new DistanceGateway.Route(4.2, 15));
        Category c = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());
        latte = coffeeRepository.save(Coffee.builder().category(c).name("Latte " + UUID.randomUUID()).price(new BigDecimal("40000")).build());
        provinceRepository.deleteAll();
        call(body(put("/api/delivery-settings"), Map.of("shopAddress", "Quán", "freeShipThreshold", 150000,
                "minOrderAmount", 50000, "openTime", "06:00", "closeTime", "22:00")));
        call(body(post("/api/delivery-areas"), Map.of("name", "Thành phố Hà Nội", "fee", 15000, "active", true,
                "wards", List.of(Map.of("name", "Xã Sóc Sơn", "fee", 35000, "blocked", false),
                        Map.of("name", "Phường Hoàn Kiếm", "blocked", true)))));
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, Object o) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(o));
    }

    private JsonNode call(MockHttpServletRequestBuilder r) throws Exception {
        var res = mvc.perform(r).andReturn().getResponse();
        assertThat(res.getStatus()).as(res.getContentAsString(StandardCharsets.UTF_8)).isLessThan(400);
        return json.readTree(res.getContentAsString(StandardCharsets.UTF_8));
    }

    private Map<String, Object> quote(int cups, String address) {
        return Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", cups)), "orderType", "DELIVERY",
                "deliveryAddress", address);
    }

    @Test
    void shippingFeeFollowsTheProvinceAndWard() throws Exception {
        JsonNode q = call(body(post("/api/orders/quote"), quote(2, BA_DINH)));
        assertThat(q.get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
        assertThat(q.get("deliveryZone").asText()).isEqualTo("Phường Ba Đình, Thành phố Hà Nội");
        assertThat(q.get("deliveryDistanceKm").decimalValue()).isEqualByComparingTo("4.2");
        // phường có phí riêng
        assertThat(call(body(post("/api/orders/quote"), quote(2, SOC_SON))).get("shippingFee").decimalValue()).isEqualByComparingTo("35000");
        // 4 × 40.000 = 160.000 ≥ 150.000: miễn phí ship
        assertThat(call(body(post("/api/orders/quote"), quote(4, SOC_SON))).get("shippingFee").decimalValue()).isEqualByComparingTo("0");
        // dưới đơn tối thiểu, phường bị chặn ("TP." = "Thành phố"), tỉnh không giao
        mvc.perform(body(post("/api/orders/quote"), quote(1, BA_DINH))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/orders/quote"), quote(2, HOAN_KIEM))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/orders/quote"), quote(2, SAI_GON))).andExpect(status().isBadRequest());
        // không đo được quãng đường vẫn tính phí
        when(distance.route(any(), any())).thenThrow(new IllegalStateException("offline"));
        assertThat(call(body(post("/api/orders/quote"), quote(2, BA_DINH))).get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
    }

    @Test
    void theAreaCheckAndPublicSettingsListWhereTheShopDelivers() throws Exception {
        assertThat(call(get("/api/public/delivery-area").param("ward", "Phường Ba Đình").param("province", "Thành phố Hà Nội"))
                .get("outside").asBoolean()).isFalse();
        JsonNode blocked = call(get("/api/public/delivery-area").param("ward", "Phường Hoàn Kiếm").param("province", "Thành phố Hà Nội"));
        assertThat(blocked.get("outside").asBoolean()).isTrue();
        assertThat(call(get("/api/public/delivery-area").param("ward", "Phường Sài Gòn").param("province", "Thành phố Hồ Chí Minh"))
                .get("message").asText()).contains("Hà Nội");
        assertThat(call(get("/api/public/delivery-settings")).get("provinces").get(0).asText()).isEqualTo("Thành phố Hà Nội");
        // the same province twice is refused
        mvc.perform(body(post("/api/delivery-areas"), Map.of("name", "TP. Hà Nội", "fee", 1, "active", true, "wards", List.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theCounterOrderIsPricedByTheAddressItGoesTo() throws Exception {
        JsonNode order = call(body(post("/api/orders"), Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 2)),
                "orderType", "DELIVERY", "customerPhone", "0912345678", "paymentMethod", "CASH",
                "delivery", Map.of("recipientName", "Lan", "recipientPhone", "0912345678", "address", SOC_SON))));
        Shipment s = shipmentRepository.findAll().stream().filter(x -> x.getOrder().getId().equals(order.get("id").asLong())).findFirst().orElseThrow();
        assertThat(s.getShippingFee()).isEqualByComparingTo("35000");
        assertThat(s.getZoneName()).isEqualTo("Xã Sóc Sơn, Thành phố Hà Nội");
        // the same address again (picked from the saved ones) is reused, not saved twice
        JsonNode again = call(body(post("/api/orders"), Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 2)),
                "orderType", "DELIVERY", "customerPhone", "0912345678", "paymentMethod", "CASH",
                "delivery", Map.of("recipientName", "Lan", "recipientPhone", "0912345678", "address", SOC_SON))));
        Shipment s2 = shipmentRepository.findAll().stream().filter(x -> x.getOrder().getId().equals(again.get("id").asLong())).findFirst().orElseThrow();
        assertThat(s2.getAddress().getId()).isEqualTo(s.getAddress().getId());
    }

    private Shipment deliveryOrder(OrderStatus status) {
        Customer cus = customerRepository.save(Customer.builder().fullName("Khách").phone("09" + (10000000 + (int) (Math.random() * 8999999))).build());
        CustomerAddress addr = addressRepository.save(CustomerAddress.builder().customer(cus).recipientName("Lan")
                .recipientPhone("0912345678").address("12 Lê Lợi, Quận 1").build());
        Order o = orderRepository.save(Order.builder().orderCode("CH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(cus).orderType(OrderType.DELIVERY).channel(OrderChannel.ONLINE).status(status)
                .subtotal(new BigDecimal("80000")).totalAmount(new BigDecimal("95000")).build());
        Shipment s = shipmentRepository.save(Shipment.builder().order(o).address(addr).shippingFee(new BigDecimal("15000")).build());
        o.setShipment(s);
        orderRepository.save(o);
        return s;
    }

    @Test
    void theShopDeliversTheReadyOrderItself() throws Exception {
        Shipment early = deliveryOrder(OrderStatus.PREPARING);
        mvc.perform(body(patch("/api/shipments/" + early.getId()), Map.of("status", "DELIVERING"))).andExpect(status().isBadRequest());

        Shipment s = deliveryOrder(OrderStatus.READY);
        call(body(patch("/api/shipments/" + s.getId()), Map.of("status", "DELIVERING")));
        assertThat(orderRepository.findById(s.getOrder().getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERING);
        call(body(patch("/api/shipments/" + s.getId()), Map.of("status", "DELIVERED")));
        assertThat(orderRepository.findById(s.getOrder().getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(shipmentRepository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(call(get("/api/shipments/" + s.getId() + "/events")).size()).isEqualTo(2);
    }

    @Test
    void aFailedDeliveryNeedsAReason() throws Exception {
        Shipment s = deliveryOrder(OrderStatus.READY);
        call(body(patch("/api/shipments/" + s.getId()), Map.of("status", "DELIVERING")));
        mvc.perform(body(patch("/api/shipments/" + s.getId()), Map.of("status", "FAILED"))).andExpect(status().isBadRequest());
        call(body(patch("/api/shipments/" + s.getId()), Map.of("status", "FAILED", "note", "Khách không nghe máy")));
        assertThat(shipmentRepository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.FAILED);
    }
}
