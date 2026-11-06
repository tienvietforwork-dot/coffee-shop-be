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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phí ship theo km (bậc phí, miễn phí từ, đơn tối thiểu, ngoài phạm vi); quán tự giao: bắt đầu giao → đã giao. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"delivery-zones", "pos", "shipments", "orders"})
class DeliveryFlowTest {

    static final DistanceGateway.Point SHOP = new DistanceGateway.Point(10.0, 106.0);
    static final DistanceGateway.Point NEAR = new DistanceGateway.Point(10.01, 106.0);
    static final DistanceGateway.Point MID = new DistanceGateway.Point(10.04, 106.0);
    static final DistanceGateway.Point FAR = new DistanceGateway.Point(10.2, 106.0);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerAddressRepository addressRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired DeliveryTimeSlotRepository slotRepository;
    @Autowired DeliveryFeePlanRepository planRepository;
    @MockBean DistanceGateway distance;
    @MockBean com.coffeeshop.integration.weather.WeatherGateway weather;

    Coffee latte;

    @BeforeEach
    void setUp() throws Exception {
        when(distance.provider()).thenReturn("test");
        when(distance.geocode(eq("Quán"), any())).thenReturn(SHOP);
        when(distance.geocode(eq("Gần"), any())).thenReturn(NEAR);
        when(distance.geocode(eq("Vừa"), any())).thenReturn(MID);
        when(distance.geocode(eq("Xa"), any())).thenReturn(FAR);
        when(distance.route(any(), eq(NEAR))).thenReturn(new DistanceGateway.Route(2.4, 8));
        when(distance.route(any(), eq(MID))).thenReturn(new DistanceGateway.Route(4.2, 15));
        when(distance.route(any(), eq(FAR))).thenReturn(new DistanceGateway.Route(22, 60));
        Category c = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());
        latte = coffeeRepository.save(Coffee.builder().category(c).name("Latte " + UUID.randomUUID()).price(new BigDecimal("40000")).build());
        slotRepository.deleteAll();
        call(body(put("/api/delivery-weather"), Map.of("enabled", false, "rainTiers", List.of())));
        planRepository.deleteAll();
        call(body(put("/api/delivery-settings"), Map.of("shopAddress", "Quán", "freeShipThreshold", 150000,
                "minOrderAmount", 50000, "openTime", "06:00", "closeTime", "22:00")));
        // the first option is the default one
        JsonNode def = call(body(post("/api/delivery-fee-plans"), plan("Mặc định", 15000, 25000)));
        assertThat(def.get("isDefault").asBoolean()).isTrue();
    }

    private Map<String, Object> plan(String name, int upTo3, int upTo7) {
        return Map.of("name", name, "tiers", List.of(Map.of("fromKm", 0, "toKm", 3, "fee", upTo3), Map.of("fromKm", 3, "toKm", 7, "fee", upTo7)));
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
    void shippingFeeFollowsTheDistanceTier() throws Exception {
        JsonNode near = call(body(post("/api/orders/quote"), quote(2, "Gần")));
        assertThat(near.get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
        assertThat(near.get("deliveryDistanceKm").decimalValue()).isEqualByComparingTo("2.4");
        assertThat(near.get("deliveryTier").asText()).isEqualTo("0–3 km");
        assertThat(call(body(post("/api/orders/quote"), quote(2, "Vừa"))).get("shippingFee").decimalValue()).isEqualByComparingTo("25000");
        // 4 × 40.000 = 160.000 ≥ 150.000: miễn phí ship
        assertThat(call(body(post("/api/orders/quote"), quote(4, "Vừa"))).get("shippingFee").decimalValue()).isEqualByComparingTo("0");
        // below the minimum, and beyond the last tier
        mvc.perform(body(post("/api/orders/quote"), quote(1, "Gần"))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/orders/quote"), quote(2, "Xa"))).andExpect(status().isBadRequest());
        // "từ X km" levels + "không giao quá": the ranges are worked out on save
        JsonNode x = call(body(post("/api/delivery-fee-plans"), Map.of("name", "X", "maxKm", 10,
                "tiers", List.of(Map.of("fromKm", 4, "fee", 2), Map.of("fromKm", 0, "fee", 1)))));
        assertThat(x.get("maxKm").decimalValue()).isEqualByComparingTo("10");
        assertThat(x.get("tiers").get(0).get("toKm").decimalValue()).isEqualByComparingTo("4");
        assertThat(x.get("tiers").get(1).get("toKm").decimalValue()).isEqualByComparingTo("10");
        // first level from 0 km; "không giao quá" beyond the last level
        mvc.perform(body(post("/api/delivery-fee-plans"), Map.of("name", "Y", "maxKm", 10, "tiers", List.of(Map.of("fromKm", 1, "fee", 1)))))
                .andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/delivery-fee-plans"), Map.of("name", "Z", "maxKm", 3, "tiers",
                List.of(Map.of("fromKm", 0, "fee", 1), Map.of("fromKm", 5, "fee", 2))))).andExpect(status().isBadRequest());
    }

    private Map<String, Object> quoteAt(int cups, String address, String time) {
        return Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", cups)), "orderType", "DELIVERY",
                "deliveryAddress", address, "deliveryTime", "2030-01-07T" + time + ":00"); // a Monday
    }

    private Map<String, Object> slot(String from, String to, long planId, List<Integer> days) {
        return Map.of("startTime", from, "endTime", to, "planId", planId, "days", days);
    }

    @Test
    void eachTimeSlotUsesItsOptionAndTheDefaultCoversTheRest() throws Exception {
        long peak = call(body(post("/api/delivery-fee-plans"), plan("Cao điểm", 20000, 30000))).get("id").asLong();
        long early = call(body(post("/api/delivery-fee-plans"), Map.of("name", "Sáng sớm",
                "tiers", List.of(Map.of("fromKm", 0, "toKm", 3, "fee", 18000))))).get("id").asLong();
        call(body(post("/api/delivery-time-slots"), slot("17:00", "20:00", peak, List.of())));
        call(body(post("/api/delivery-time-slots"), slot("06:00", "08:00", early, List.of(1, 2, 3, 4, 5))));
        // an option is reusable in several slots
        call(body(post("/api/delivery-time-slots"), slot("11:00", "13:00", peak, List.of(6, 7))));

        // outside every slot: option mặc định
        JsonNode noon = call(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00")));
        assertThat(noon.get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
        assertThat(noon.has("deliveryOption")).isFalse();
        JsonNode evening = call(body(post("/api/orders/quote"), quoteAt(2, "Vừa", "18:30")));
        assertThat(evening.get("shippingFee").decimalValue()).isEqualByComparingTo("30000");
        assertThat(evening.get("deliveryOption").asText()).isEqualTo("Cao điểm");
        assertThat(evening.get("deliveryNote").asText()).isEqualTo("Phí giao hàng khung giờ Cao điểm");
        assertThat(noon.has("deliveryNote")).isFalse();
        // 6–8h on a Monday: Sáng sớm; its only range stops at 3 km, so 4.2 km is out of range then
        assertThat(call(body(post("/api/orders/quote"), quoteAt(2, "Gần", "07:00"))).get("shippingFee").decimalValue()).isEqualByComparingTo("18000");
        mvc.perform(body(post("/api/orders/quote"), quoteAt(2, "Vừa", "07:00"))).andExpect(status().isBadRequest());

        // overlapping slots are refused; an option in use, or the default one, cannot be deleted
        mvc.perform(body(post("/api/delivery-time-slots"), slot("19:00", "21:00", early, List.of()))).andExpect(status().isBadRequest());
        // outside the opening hours (06:00–22:00)
        mvc.perform(body(post("/api/delivery-time-slots"), slot("05:00", "07:00", early, List.of()))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/delivery-time-slots"), slot("21:00", "23:00", early, List.of()))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/delivery-fee-plans/" + peak)).andExpect(status().isBadRequest());
        long def = planRepository.findFirstByDefaultPlanTrue().orElseThrow().getId();
        mvc.perform(delete("/api/delivery-fee-plans/" + def)).andExpect(status().isBadRequest());
        assertThat(call(get("/api/delivery-time-slots")).size()).isEqualTo(3);

        // another option becomes the default; the old one loses the flag; the default cannot just be unset
        call(body(put("/api/delivery-fee-plans/" + early), Map.of("name", "Sáng sớm", "isDefault", true,
                "tiers", List.of(Map.of("fromKm", 0, "toKm", 3, "fee", 18000)))));
        assertThat(planRepository.findFirstByDefaultPlanTrue().orElseThrow().getId()).isEqualTo(early);
        assertThat(planRepository.findById(def).orElseThrow().isDefaultPlan()).isFalse();
        mvc.perform(body(put("/api/delivery-fee-plans/" + early), Map.of("name", "Sáng sớm", "isDefault", false,
                "tiers", List.of(Map.of("fromKm", 0, "toKm", 3, "fee", 18000))))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/delivery-fee-plans/" + early)).andExpect(status().isBadRequest());
        assertThat(call(get("/api/public/delivery-settings")).get("openTime").asText()).startsWith("06:00");
    }

    private void rain(double mmPerHour, int code) {
        when(weather.at(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(), any()))
                .thenReturn(new com.coffeeshop.integration.weather.WeatherGateway.Weather(null, code, 27, mmPerHour, 10));
    }

    @Test
    void badWeatherAddsTheRainSurchargeOrStopsDeliveries() throws Exception {
        call(body(put("/api/delivery-weather"), Map.of("enabled", true, "stopRainMm", 30,
                "rainTiers", List.of(Map.of("minRainMm", 2, "surcharge", 5000), Map.of("minRainMm", 10, "surcharge", 10000)))));
        rain(0.5, 61);
        assertThat(call(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00"))).get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
        rain(5, 63); // bậc 2 mm/h
        JsonNode q = call(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00")));
        assertThat(q.get("shippingFee").decimalValue()).isEqualByComparingTo("20000");
        assertThat(q.get("weatherSurcharge").decimalValue()).isEqualByComparingTo("5000");
        assertThat(q.get("deliveryNote").asText()).startsWith("Phụ phí do thời tiết xấu");
        rain(12, 65); // bậc 10 mm/h; free ship (4 × 40.000 ≥ 150.000) still pays the surcharge
        assertThat(call(body(post("/api/orders/quote"), quoteAt(4, "Gần", "12:00"))).get("shippingFee").decimalValue()).isEqualByComparingTo("10000");
        rain(3, 95); // giông: không ai đi giao
        mvc.perform(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00"))).andExpect(status().isBadRequest());
        rain(35, 65); // ≥ 30 mm/h: no deliveries
        mvc.perform(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00"))).andExpect(status().isBadRequest());
        // switched off: no effect
        call(body(put("/api/delivery-weather"), Map.of("enabled", false, "rainTiers", List.of())));
        assertThat(call(body(post("/api/orders/quote"), quoteAt(2, "Gần", "12:00"))).get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
    }

    @Test
    void theCounterOrderIsPricedByTheAddressItGoesTo() throws Exception {
        JsonNode order = call(body(post("/api/orders"), Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 2)),
                "orderType", "DELIVERY", "customerPhone", "0912345678", "paymentMethod", "CASH",
                "delivery", Map.of("recipientName", "Lan", "recipientPhone", "0912345678", "address", "Vừa"))));
        Shipment s = shipmentRepository.findAll().stream().filter(x -> x.getOrder().getId().equals(order.get("id").asLong())).findFirst().orElseThrow();
        assertThat(s.getShippingFee()).isEqualByComparingTo("25000");
        assertThat(s.getDistanceKm()).isEqualByComparingTo("4.2");
        assertThat(s.getZoneName()).isEqualTo("3–7 km");
        // the same address again (picked from the saved ones) is reused, not saved twice
        JsonNode again = call(body(post("/api/orders"), Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 2)),
                "orderType", "DELIVERY", "customerPhone", "0912345678", "paymentMethod", "CASH",
                "delivery", Map.of("recipientName", "Lan", "recipientPhone", "0912345678", "address", "Vừa"))));
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
