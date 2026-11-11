package com.coffeeshop.controller;

import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.*;
import com.coffeeshop.integration.distance.DistanceGateway;
import com.coffeeshop.integration.vnpay.VnpayGateway;
import com.coffeeshop.repository.*;
import com.coffeeshop.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Số lượng nguyên liệu tự trừ / tự hết món; phân công người giao + tuyến giao; gói giao định kỳ tự tạo đơn; lịch hẹn
 * giao; từ chối đơn đã trả online khi VNPAY không hoàn được → chờ hoàn tay.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"materials", "materials.edit", "menu", "menu.edit", "pos", "orders", "orders.refund",
        "shipments", "delivery-zones", "subscriptions", "delivery-schedule"})
class OperationsFeaturesFlowTest {

    static final String ADDRESS = "12 Điện Biên Phủ, Phường Ba Đình, Thành phố Hà Nội";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentTransactionRepository txRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired DeliveryProvinceRepository provinceRepository;
    @Autowired SubscriptionService subscriptionService;
    @MockBean DistanceGateway distance;
    @SpyBean VnpayGateway vnpay;

    Coffee latte;

    @BeforeEach
    void setUp() throws Exception {
        when(distance.geocode(any(), any())).thenReturn(new DistanceGateway.Point(21.03, 105.85));
        when(distance.route(any(), any())).thenReturn(new DistanceGateway.Route(3, 10));
        Category c = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());
        latte = coffeeRepository.save(Coffee.builder().category(c).name("Latte " + UUID.randomUUID()).price(new BigDecimal("40000")).build());
        provinceRepository.deleteAll();
        call(body(put("/api/delivery-settings"), Map.of("shopAddress", "Quán", "openTime", "00:00", "closeTime", "23:59")));
        call(body(post("/api/delivery-areas"), Map.of("name", "Thành phố Hà Nội", "fee", 15000, "active", true, "wards", List.of())));
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, Object o) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(o));
    }

    private JsonNode call(MockHttpServletRequestBuilder r) throws Exception {
        var res = mvc.perform(r).andReturn().getResponse();
        assertThat(res.getStatus()).as(res.getContentAsString(StandardCharsets.UTF_8)).isLessThan(400);
        String s = res.getContentAsString(StandardCharsets.UTF_8);
        return s.isEmpty() ? null : json.readTree(s);
    }

    private JsonNode counterOrder(int cups, String type) throws Exception {
        var req = new java.util.HashMap<String, Object>(Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", cups)),
                "orderType", type, "paymentMethod", "CASH", "customerPhone", "0987654321"));
        if (type.equals("DELIVERY")) req.put("delivery", Map.of("recipientName", "Lan", "recipientPhone", "0987654321", "address", ADDRESS));
        return call(body(post("/api/orders"), req));
    }

    @Test
    void ordersUseUpMaterialsAndTheCoffeeSellsOutThenComesBack() throws Exception {
        long milk = call(body(post("/api/materials"), Map.of("name", "Sữa " + UUID.randomUUID(), "unit", "ml", "quantity", 250))).get("id").asLong();
        call(body(post("/api/coffees/" + latte.getId() + "/recipes"), Map.of("brewTimeMin", 3,
                "materials", List.of(Map.of("materialId", milk, "quantity", 100)), "steps", List.of())));
        // 2 ly: 250 → 50 ml, không đủ cho ly thứ 3 → tự hết món
        counterOrder(2, "TAKE_AWAY");
        assertThat(materialRepository.findById(milk).orElseThrow().getQuantity()).isEqualByComparingTo("50");
        Coffee c = coffeeRepository.findById(latte.getId()).orElseThrow();
        assertThat(c.getStatus()).isEqualTo(CoffeeStatus.SOLD_OUT);
        assertThat(c.isAutoSoldOut()).isTrue();
        // hết món: không bán được nữa
        mvc.perform(body(post("/api/orders"), Map.of("items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 1)),
                "orderType", "TAKE_AWAY", "paymentMethod", "CASH"))).andExpect(status().isBadRequest());
        // thêm 500 ml → tự mở bán lại; không cho bớt quá số đang có
        JsonNode m = call(body(patch("/api/materials/" + milk + "/quantity"), Map.of("delta", 500)));
        assertThat(m.get("quantity").decimalValue()).isEqualByComparingTo("550");
        assertThat(m.get("cupsLeft").asInt()).isEqualTo(5);
        assertThat(coffeeRepository.findById(latte.getId()).orElseThrow().getStatus()).isEqualTo(CoffeeStatus.AVAILABLE);
        mvc.perform(body(patch("/api/materials/" + milk + "/quantity"), Map.of("delta", -600))).andExpect(status().isBadRequest());
        // nhân viên tự đánh "hết món" thì hệ thống không tự mở lại
        call(body(patch("/api/coffees/" + latte.getId() + "/status"), Map.of("status", "SOLD_OUT")));
        call(body(patch("/api/materials/" + milk + "/quantity"), Map.of("delta", 100)));
        assertThat(coffeeRepository.findById(latte.getId()).orElseThrow().getStatus()).isEqualTo(CoffeeStatus.SOLD_OUT);
    }

    @Test
    void theManagerAssignsAShipperAndSeesTheRoute() throws Exception {
        Staff lan = staffRepository.save(Staff.builder().fullName("Lan giao hàng").phone("0900000009").build());
        long orderId = counterOrder(2, "DELIVERY").get("id").asLong();
        long shipmentId = orderRepository.findById(orderId).orElseThrow().getShipment().getId();
        JsonNode s = call(body(put("/api/shipments/" + shipmentId + "/shipper"), Map.of("staffId", lan.getId())));
        assertThat(s.get("shipperName").asText()).isEqualTo("Lan giao hàng");
        JsonNode stats = call(get("/api/shippers"));
        JsonNode mine = null;
        for (JsonNode x : stats) if (x.get("id").asLong() == lan.getId()) mine = x;
        assertThat(mine).isNotNull();
        assertThat(mine.get("pending").asInt()).isEqualTo(1);
        // tuyến giao: quán → điểm giao, link Google Maps chỉ đường
        JsonNode plan = call(body(post("/api/shipments/route"), Map.of("shipmentIds", List.of(shipmentId))));
        assertThat(plan.get("stops").size()).isEqualTo(1);
        assertThat(plan.get("googleMapsUrl").asText()).startsWith("https://www.google.com/maps/dir/?api=1").contains("destination=");
    }

    @Test
    void aSubscriptionCreatesTheDeliveryOrderBeforeItsTime() throws Exception {
        LocalDateTime now = LocalDateTime.now(com.coffeeshop.service.DeliveryFeeService.SHOP_ZONE);
        String at = now.plusMinutes(40).toLocalTime().withSecond(0).withNano(0).toString();
        JsonNode sub = call(body(post("/api/subscriptions"), Map.of("customerPhone", "0911222333", "customerName", "Minh",
                "delivery", Map.of("recipientName", "Minh", "recipientPhone", "0911222333", "address", ADDRESS),
                "items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 2)), "frequency", "DAILY",
                "deliveryTime", at, "startDate", now.toLocalDate().toString())));
        long id = sub.get("id").asLong();
        assertThat(sub.get("schedule").asText()).startsWith("Mỗi ngày lúc");
        // còn 40 phút (< 60 phút): đến hạn → tạo 1 đơn giao, chạy lại không tạo thêm
        assertThat(subscriptionService.due(now)).contains(id);
        subscriptionService.runDue(id, now);
        subscriptionService.runDue(id, now);
        List<Order> created = orderRepository.findAll().stream().filter(o -> Long.valueOf(id).equals(o.getSubscriptionId())).toList();
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getOrderType()).isEqualTo(OrderType.DELIVERY);
        assertThat(created.get(0).getPickupTime()).isNotNull();
        // xuất hiện trong lịch hẹn giao hôm nay
        JsonNode rows = call(get("/api/delivery-schedule"));
        boolean found = false;
        for (JsonNode r : rows) if (created.get(0).getOrderCode().equals(r.path("orderCode").asText())) found = true;
        assertThat(found).isTrue();
        // tuần: phải chọn thứ
        mvc.perform(body(post("/api/subscriptions"), Map.of("customerPhone", "0911222333",
                "delivery", Map.of("recipientName", "Minh", "recipientPhone", "0911222333", "address", ADDRESS),
                "items", List.of(Map.of("coffeeId", latte.getId(), "quantity", 1)), "frequency", "WEEKLY", "days", List.of(),
                "deliveryTime", "08:00", "startDate", LocalDate.now().toString()))).andExpect(status().isBadRequest());
    }

    @Test
    void rejectingAPaidOrderThatVnpayCannotRefundWaitsForAManualTransfer() throws Exception {
        Order o = orderRepository.save(Order.builder().orderCode("CH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderType(OrderType.TAKE_AWAY).channel(OrderChannel.ONLINE).subtotal(new BigDecimal("59000"))
                .totalAmount(new BigDecimal("59000")).build());
        Payment p = paymentRepository.save(Payment.builder().order(o).method(PaymentMethod.BANK_TRANSFER).amount(new BigDecimal("59000"))
                .status(PaymentStatus.PAID).paidAt(LocalDateTime.now()).build());
        o.getPayments().add(p);
        txRepository.save(PaymentTransaction.builder().payment(p).provider("VNPAY").type("PAY").txnRef("T" + UUID.randomUUID().toString().substring(0, 12))
                .amount(new BigDecimal("59000")).status(PaymentTransactionStatus.SUCCESS).providerTxnNo("1234").gatewayDate("20261111090000")
                .createdAt(LocalDateTime.now()).build());
        // VNPAY từ chối hoàn
        doReturn(false).when(vnpay).simulated();
        doReturn(new VnpayGateway.Answer("94", "", null, null, "refused")).when(vnpay)
                .refund(anyString(), any(), anyBoolean(), any(), any(), any(), any());

        call(body(post("/api/orders/" + o.getId() + "/reject"), Map.of("reason", "Hết nguyên liệu")));
        assertThat(orderRepository.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REJECTED);
        Payment pending = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(pending.getRefundNote()).contains("cần chuyển khoản");
        JsonNode list = call(get("/api/payments/refund-pending"));
        assertThat(list.findValuesAsText("orderCode")).contains(o.getOrderCode());

        // không có mã giao dịch: không cho đánh dấu
        mvc.perform(body(post("/api/payments/" + p.getId() + "/manual-refund"), Map.of("reference", " "))).andExpect(status().isBadRequest());
        JsonNode done = call(body(post("/api/payments/" + p.getId() + "/manual-refund"), Map.of("reference", "FT2611110001", "note", "VCB")));
        assertThat(done.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(done.get("refundRef").asText()).isEqualTo("FT2611110001");
        assertThat(done.get("refundAmount").decimalValue()).isEqualByComparingTo("59000");
    }
}
