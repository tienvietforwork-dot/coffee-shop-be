package com.coffeeshop.controller;

import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.*;
import com.coffeeshop.repository.*;
import com.coffeeshop.service.CustomerNotificationService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Đơn hoàn tất → hóa đơn điện tử (phát hành, thay thế khi khách bổ sung MST, điều chỉnh khi hoàn tiền) + tin ZNS cho khách. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"orders", "orders.refund", "invoices", "notifications"})
class InvoiceNotificationFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CoffeeRepository coffeeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired NotificationTemplateRepository templateRepository;
    @Autowired NotificationMessageRepository messageRepository;
    @Autowired CustomerNotificationService notificationService;

    Order order;
    Payment payment;

    @BeforeEach
    void setUp() {
        for (String code : new String[]{"ORDER_CONFIRMED", "ORDER_READY", "ORDER_DELIVERING", "ORDER_COMPLETED", "ORDER_CANCELLED"}) {
            templateRepository.save(NotificationTemplate.builder().code(code).name(code).enabled(true).znsTemplateId("2301")
                    .smsContent("Coffeeholic: don {order_code} cua {customer_name} - {status}. Xem {link}").smsFallback(true).build());
        }
        Category c = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());
        Coffee latte = coffeeRepository.save(Coffee.builder().category(c).name("Bạc xỉu").price(new BigDecimal("54000")).build());
        Customer cus = customerRepository.save(Customer.builder().fullName("Nguyễn Lan").phone("09" + (10000000 + (int) (Math.random() * 8999999))).build());
        order = Order.builder().orderCode("CH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()).customer(cus)
                .orderType(OrderType.TAKE_AWAY).channel(OrderChannel.ONLINE).status(OrderStatus.READY)
                .subtotal(new BigDecimal("108000")).discountAmount(new BigDecimal("8000")).totalAmount(new BigDecimal("100000")).build();
        order.getItems().add(OrderItem.builder().order(order).coffee(latte).quantity(2).unitPrice(new BigDecimal("54000"))
                .lineTotal(new BigDecimal("108000")).note("ít đá").build());
        order = orderRepository.save(order);
        payment = paymentRepository.save(Payment.builder().order(order).method(PaymentMethod.CASH).amount(new BigDecimal("100000"))
                .status(PaymentStatus.PAID).paidAt(LocalDateTime.now()).build());
        order.getPayments().add(payment);
    }

    private JsonNode call(MockHttpServletRequestBuilder r, Object body) throws Exception {
        if (body != null) r = r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var res = mvc.perform(r).andReturn().getResponse();
        assertThat(res.getStatus()).as(res.getContentAsString(StandardCharsets.UTF_8)).isLessThan(400);
        return json.readTree(res.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void completedOrderIsInvoicedReplacedAndAdjusted() throws Exception {
        call(post("/api/orders/" + order.getId() + "/complete"), null);
        JsonNode invoices = call(get("/api/public/orders/" + order.getOrderCode() + "/invoices"), null);
        assertThat(invoices.size()).isEqualTo(1);
        JsonNode inv = invoices.get(0);
        assertThat(inv.get("status").asText()).isEqualTo("PENDING");
        assertThat(inv.get("series").asText()).startsWith("2C26M"); // hóa đơn bán hàng từ máy tính tiền
        // each drink on its own line, then the discount
        assertThat(inv.at("/lines/0/name").asText()).contains("Bạc xỉu");
        assertThat(inv.at("/lines/1/amount").decimalValue()).isEqualByComparingTo("-8000");
        assertThat(inv.get("totalAmount").decimalValue()).isEqualByComparingTo("100000");

        call(post("/api/invoices/issue-now"), null);
        inv = call(get("/api/invoices/" + inv.get("id").asLong()), null);
        assertThat(inv.get("status").asText()).isEqualTo("ISSUED");
        assertThat(inv.get("invoiceNo").asText()).hasSize(8);
        assertThat(inv.get("taxAuthorityCode").asText()).isNotBlank();
        long originalId = inv.get("id").asLong();

        // the customer asks for a company invoice afterwards: a replacement, never a cancellation
        mvc.perform(post("/api/public/orders/" + order.getOrderCode() + "/invoice-request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"buyerTaxCode\":\"123\",\"buyerCompany\":\"X\",\"buyerAddress\":\"Y\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        JsonNode repl = call(post("/api/public/orders/" + order.getOrderCode() + "/invoice-request"),
                Map.of("buyerCompany", "Công ty TNHH ABC", "buyerTaxCode", "0101234567", "buyerAddress", "Hà Nội", "buyerEmail", "kt@abc.vn"));
        assertThat(repl.get("kind").asText()).isEqualTo("REPLACEMENT");
        call(post("/api/invoices/issue-now"), null);
        assertThat(call(get("/api/invoices/" + originalId), null).get("status").asText()).isEqualTo("REPLACED");
        assertThat(call(get("/api/invoices/" + repl.get("id").asLong()), null).get("status").asText()).isEqualTo("ISSUED");

        // money back after invoicing → hóa đơn điều chỉnh giảm
        call(post("/api/payments/" + payment.getId() + "/refund"), Map.of("amount", 20000));
        JsonNode all = call(get("/api/public/orders/" + order.getOrderCode() + "/invoices"), null);
        JsonNode adj = all.get(all.size() - 1);
        assertThat(adj.get("kind").asText()).isEqualTo("ADJUSTMENT");
        assertThat(adj.get("totalAmount").decimalValue()).isEqualByComparingTo("-20000");

        String today = LocalDate.now().toString();
        assertThat(call(get("/api/invoices").param("from", today).param("to", today).param("keyword", order.getOrderCode()), null).size()).isEqualTo(3);
    }

    @Test
    void theCustomerGetsAZnsWhenTheOrderIsCompleted() throws Exception {
        call(post("/api/orders/" + order.getId() + "/complete"), null);
        var queued = messageRepository.findByRequestId(order.getOrderCode() + "-ORDER_COMPLETED").orElseThrow();
        assertThat(queued.getStatus()).isEqualTo("PENDING");
        assertThat(queued.getPhone()).startsWith("84");
        assertThat(queued.getContent()).contains(order.getOrderCode()).contains("Nguyen Lan"); // SMS without accents

        notificationService.sendQueued();
        var sent = messageRepository.findByRequestId(order.getOrderCode() + "-ORDER_COMPLETED").orElseThrow();
        assertThat(sent.getStatus()).isEqualTo("SENT");
        assertThat(sent.getChannel()).isEqualTo("ZNS");

        String today = LocalDate.now().toString();
        assertThat(call(get("/api/notifications/messages").param("from", today).param("to", today), null).size()).isGreaterThanOrEqualTo(1);
        // a template switched off sends nothing
        call(put("/api/notifications/templates/ORDER_CANCELLED"), Map.of("enabled", false));
        assertThat(call(get("/api/notifications/templates"), null).size()).isEqualTo(5);
    }
}
