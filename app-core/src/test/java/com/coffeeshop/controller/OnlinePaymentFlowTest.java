package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.enums.*;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.PaymentRepository;
import com.coffeeshop.repository.PaymentTransactionRepository;
import com.coffeeshop.scheduler.PaymentExpiryScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Đơn online trả qua VNPAY (giả lập): link → trang thanh toán → IPN → return → đối soát → hoàn tiền; link quá hạn → hủy đơn. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = {"orders", "orders.refund", "payment-reconciliation"})
class OnlinePaymentFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentTransactionRepository txRepository;
    @Autowired PaymentExpiryScheduler expiryScheduler;

    private Order onlineOrder(int amount) {
        Order o = orderRepository.save(Order.builder().orderCode("CH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderType(OrderType.TAKE_AWAY).channel(OrderChannel.ONLINE).subtotal(BigDecimal.valueOf(amount))
                .totalAmount(BigDecimal.valueOf(amount)).build());
        Payment p = paymentRepository.save(Payment.builder().order(o).method(PaymentMethod.E_WALLET).amount(BigDecimal.valueOf(amount)).build());
        o.getPayments().add(p);
        return o;
    }

    private JsonNode call(RequestBuilder r) throws Exception {
        return json.readTree(mvc.perform(r).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Map<String, String> params(String url) {
        Map<String, String> m = new LinkedHashMap<>();
        UriComponentsBuilder.fromUriString(url).build().getQueryParams()
                .forEach((k, v) -> m.put(k, URLDecoder.decode(v.get(0), StandardCharsets.US_ASCII)));
        return m;
    }

    @Test
    void paidThroughVnpayThenReconciledAndRefunded() throws Exception {
        Order order = onlineOrder(59000);
        // staff cannot start an online order the customer has not paid
        mvc.perform(post("/api/orders/" + order.getId() + "/confirm")).andExpect(status().isBadRequest());

        JsonNode link = call(post("/api/public/orders/" + order.getOrderCode() + "/pay"));
        String url = link.get("paymentUrl").asText();
        assertThat(url).contains("/api/public/payments/sandbox?").contains("vnp_Amount=5900000").contains("vnp_SecureHash=");
        Map<String, String> request = params(url);
        mvc.perform(get("/api/public/payments/sandbox").params(org.springframework.util.CollectionUtils.toMultiValueMap(
                request.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> java.util.List.of(e.getValue()))))))
                .andExpect(status().isOk());

        // the customer pays on the (simulated) VNPAY page → IPN → redirect to the return URL
        var post = post("/api/public/payments/sandbox/complete").param("result", "00").contentType(MediaType.APPLICATION_FORM_URLENCODED);
        request.forEach(post::param);
        String location = mvc.perform(post).andExpect(status().isFound()).andReturn().getResponse().getHeader("Location");
        Map<String, String> back = params(location);
        assertThat(back.get("vnp_ResponseCode")).isEqualTo("00");

        var ret = get("/api/public/payments/vnpay/return");
        back.forEach(ret::param);
        JsonNode result = call(ret);
        assertThat(result.get("validSignature").asBoolean()).isTrue();
        assertThat(result.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(paymentRepository.findById(order.getPayments().get(0).getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);

        // VNPAY retries the IPN: already confirmed; a forged one is refused
        var again = get("/api/public/payments/vnpay/ipn");
        back.forEach(again::param);
        assertThat(call(again).get("RspCode").asText()).isEqualTo("02");
        var forged = get("/api/public/payments/vnpay/ipn");
        back.forEach((k, v) -> forged.param(k, k.equals("vnp_Amount") ? "100" : v));
        assertThat(call(forged).get("RspCode").asText()).isEqualTo("97");

        String today = LocalDate.now().toString();
        JsonNode list = call(get("/api/payment-transactions").param("from", today).param("to", today));
        JsonNode tx = null;
        for (JsonNode t : list) if (t.get("orderCode").asText().equals(order.getOrderCode())) tx = t;
        assertThat(tx).isNotNull();
        assertThat(tx.has("mismatch")).isFalse();
        JsonNode rec = call(post("/api/payment-transactions/" + tx.get("id").asLong() + "/reconcile")
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Khớp sao kê\"}"));
        assertThat(rec.get("reconciled").asBoolean()).isTrue();

        // partial refund goes back through VNPAY
        long paymentId = order.getPayments().get(0).getId();
        mvc.perform(post("/api/payments/" + paymentId + "/refund").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":20000}"))
                .andExpect(status().isOk());
        assertThat(txRepository.findByPaymentIdOrderByIdDesc(paymentId).get(0).getType()).isEqualTo("REFUND");
        assertThat(txRepository.findByPaymentIdOrderByIdDesc(paymentId).get(0).getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
    }

    @Test
    void unpaidLinkExpiresAndTheOrderIsCancelled() throws Exception {
        Order order = onlineOrder(35000);
        JsonNode link = call(post("/api/public/orders/" + order.getOrderCode() + "/pay"));
        var t = txRepository.findByTxnRef(link.get("txnRef").asText()).orElseThrow();
        t.setExpireAt(LocalDateTime.now().minusMinutes(1));
        txRepository.saveAndFlush(t);

        expiryScheduler.expireUnpaid();

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(txRepository.findByTxnRef(link.get("txnRef").asText()).orElseThrow().getStatus()).isEqualTo(PaymentTransactionStatus.EXPIRED);
    }
}
