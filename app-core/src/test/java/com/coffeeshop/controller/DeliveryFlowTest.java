package com.coffeeshop.controller;

import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.*;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phí ship theo khu vực (miễn phí từ, đơn tối thiểu, ngoài vùng); đặt xe Ahamove → webhook → đơn hoàn tất; hủy chuyến. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"delivery-zones", "pos", "shipments", "orders"})
class DeliveryFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerAddressRepository addressRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired DeliveryZoneRepository zoneRepository;

    Coffee latte;

    @BeforeEach
    void setUp() throws Exception {
        zoneRepository.deleteAll();
        Category c = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());
        latte = coffeeRepository.save(Coffee.builder().category(c).name("Latte " + UUID.randomUUID()).price(new BigDecimal("40000")).build());
        mvc.perform(body(post("/api/delivery-zones"), Map.of("name", "Nội thành gần", "districts", List.of("Quận 1", "Quận 3"),
                "fee", 15000, "freeShipThreshold", 150000, "minOrderAmount", 50000, "estimatedMinutes", 25))).andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, Object o) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(o));
    }

    private JsonNode call(MockHttpServletRequestBuilder r) throws Exception {
        var res = mvc.perform(r).andReturn().getResponse();
        assertThat(res.getStatus()).as(res.getContentAsString(StandardCharsets.UTF_8)).isLessThan(400);
        return json.readTree(res.getContentAsString(StandardCharsets.UTF_8));
    }

    private Map<String, Object> quote(int cups, String district) {
        return Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", cups)), "orderType", "DELIVERY",
                "deliveryDistrict", district);
    }

    @Test
    void shippingFeeFollowsTheZone() throws Exception {
        assertThat(call(body(post("/api/orders/quote"), quote(2, "quận 1"))).get("shippingFee").decimalValue()).isEqualByComparingTo("15000");
        // 4 × 40.000 = 160.000 ≥ 150.000: miễn phí ship
        assertThat(call(body(post("/api/orders/quote"), quote(4, "Quận 3"))).get("shippingFee").decimalValue()).isEqualByComparingTo("0");
        // below the zone's minimum, and outside every zone
        mvc.perform(body(post("/api/orders/quote"), quote(1, "Quận 1"))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/orders/quote"), quote(2, "Quận 9"))).andExpect(status().isBadRequest());
        // a district belongs to one zone only
        mvc.perform(body(post("/api/delivery-zones"), Map.of("name", "Khác", "districts", List.of("quan 1"), "fee", 20000)))
                .andExpect(status().isBadRequest());
        assertThat(call(get("/api/public/delivery-zones")).get(0).get("districts").size()).isEqualTo(2);
    }

    private Shipment deliveryOrder(OrderStatus status) {
        Customer cus = customerRepository.save(Customer.builder().fullName("Khách").phone("09" + (10000000 + (int) (Math.random() * 8999999))).build());
        CustomerAddress addr = addressRepository.save(CustomerAddress.builder().customer(cus).recipientName("Lan")
                .recipientPhone("0912345678").address("12 Lê Lợi, Quận 1").build());
        Order o = orderRepository.save(Order.builder().orderCode("CH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(cus).orderType(OrderType.DELIVERY).channel(OrderChannel.ONLINE).status(status)
                .subtotal(new BigDecimal("80000")).totalAmount(new BigDecimal("95000")).build());
        Shipment s = shipmentRepository.save(Shipment.builder().order(o).address(addr).shippingFee(new BigDecimal("15000")).district("Quận 1").build());
        o.setShipment(s);
        return s;
    }

    private void webhook(String orderId, String status, String token) throws Exception {
        mvc.perform(body(post("/api/public/delivery/ahamove/webhook").param("token", token),
                Map.of("_id", orderId, "status", status, "supplier_name", "Anh Tú", "supplier_id", "0909000111",
                        "path", List.of(Map.of("status", "COMPLETED"), Map.of("status", "COMPLETED")))));
    }

    @Test
    void bookedWithAhamoveAndDrivenToCompletionByTheWebhook() throws Exception {
        Shipment s = deliveryOrder(OrderStatus.PREPARING);
        JsonNode estimate = call(get("/api/shipments/" + s.getId() + "/ahamove/estimate"));
        assertThat(estimate.get("fee").decimalValue()).isPositive();

        JsonNode booked = call(post("/api/shipments/" + s.getId() + "/ahamove/book"));
        assertThat(booked.get("status").asText()).isEqualTo("BOOKED");
        assertThat(booked.get("partner").asText()).isEqualTo("AHAMOVE");
        String partnerId = booked.get("partnerOrderId").asText();

        // wrong token: refused
        mvc.perform(body(post("/api/public/delivery/ahamove/webhook").param("token", "x"), Map.of("_id", partnerId, "status", "ACCEPTED")))
                .andExpect(status().isUnauthorized());
        webhook(partnerId, "ACCEPTED", "coffeeholic-webhook");
        assertThat(shipmentRepository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.DRIVER_ACCEPTED);
        assertThat(shipmentRepository.findById(s.getId()).orElseThrow().getDriverName()).isEqualTo("Anh Tú");

        Order o = orderRepository.findById(s.getOrder().getId()).orElseThrow();
        o.setStatus(OrderStatus.READY);
        orderRepository.saveAndFlush(o);
        webhook(partnerId, "IN PROCESS", "coffeeholic-webhook");
        assertThat(orderRepository.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERING);
        webhook(partnerId, "COMPLETED", "coffeeholic-webhook");
        assertThat(shipmentRepository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(orderRepository.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(call(get("/api/shipments/" + s.getId() + "/events")).size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void aBookingIsCancelledBeforePickUpAndCanBeBookedAgain() throws Exception {
        Shipment s = deliveryOrder(OrderStatus.PREPARING);
        call(post("/api/shipments/" + s.getId() + "/ahamove/book"));
        JsonNode cancelled = call(body(post("/api/shipments/" + s.getId() + "/ahamove/cancel"), Map.of("reason", "Khách đổi giờ")));
        assertThat(cancelled.get("status").asText()).isEqualTo("PENDING");
        assertThat(call(post("/api/shipments/" + s.getId() + "/ahamove/book")).get("status").asText()).isEqualTo("BOOKED");
        // manual updates are refused while Ahamove drives the trip
        mvc.perform(body(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/shipments/" + s.getId()),
                Map.of("status", "DELIVERED"))).andExpect(status().isBadRequest());
    }

    @Test
    void anOutsideDriverTakesTheReadyOrderAndDeliversIt() throws Exception {
        Shipment s = deliveryOrder(OrderStatus.READY);
        var patch = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/shipments/" + s.getId());
        call(body(patch, Map.of("status", "DELIVERING", "carrier", "GrabExpress", "trackingCode", "GRAB-1")));
        assertThat(orderRepository.findById(s.getOrder().getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERING);
        call(body(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/shipments/" + s.getId()),
                Map.of("status", "DELIVERED")));
        assertThat(orderRepository.findById(s.getOrder().getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
