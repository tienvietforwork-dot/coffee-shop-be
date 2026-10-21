package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.enums.OrderChannel;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.repository.CashShiftRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.PaymentRepository;
import com.coffeeshop.repository.StaffRepository;
import com.coffeeshop.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mở ca → bán (tiền mặt + chuyển khoản) → chi trong ca → chốt ca lệch tiền; phân ca → tự chấm công; nhật ký thao tác. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffOperationsFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StaffRepository staffRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired CashShiftRepository shiftRepository;

    Staff staff;
    UsernamePasswordAuthenticationToken manager;

    @BeforeEach
    void setUp() {
        // one register: close whatever another test left open
        shiftRepository.findFirstByClosedAtIsNullOrderByIdDesc().ifPresent(s -> {
            s.setClosedAt(LocalDateTime.now().minusMinutes(1));
            shiftRepository.saveAndFlush(s);
        });
        staff = staffRepository.save(Staff.builder().fullName("Lan " + UUID.randomUUID().toString().substring(0, 6)).phone("0900000001").build());
        var principal = new UserPrincipal(1L, "lan", "x", true, List.of("STAFF"),
                Set.of("cash-shifts", "pos", "work-shifts", "staff", "audit-logs", "orders.refund"), staff.getId(), null);
        manager = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private JsonNode call(MockHttpServletRequestBuilder r, Object body) throws Exception {
        if (body != null) r = r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var res = mvc.perform(r.with(authentication(manager))).andReturn().getResponse();
        assertThat(res.getStatus()).as(res.getContentAsString(StandardCharsets.UTF_8)).isLessThan(400);
        String s = res.getContentAsString(StandardCharsets.UTF_8);
        return s.isBlank() ? null : json.readTree(s);
    }

    private Payment paid(PaymentMethod method, int amount) {
        Order o = orderRepository.save(Order.builder().orderCode("T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderType(OrderType.TAKE_AWAY).channel(OrderChannel.COUNTER).subtotal(BigDecimal.valueOf(amount))
                .totalAmount(BigDecimal.valueOf(amount)).build());
        return paymentRepository.save(Payment.builder().order(o).method(method).amount(BigDecimal.valueOf(amount))
                .status(PaymentStatus.PAID).paidAt(LocalDateTime.now()).build());
    }

    @Test
    void aCashRefundLeavesTheTill() throws Exception {
        call(post("/api/cash-shifts/open"), Map.of("openingCash", 500000));
        Payment p = paid(PaymentMethod.CASH, 117000);
        call(post("/api/payments/" + p.getId() + "/refund"), Map.of("amount", 39000));
        JsonNode s = call(get("/api/cash-shifts/current"), null);
        // 500.000 + 117.000 − 39.000 handed back = 578.000
        assertThat(s.get("expectedCash").decimalValue()).isEqualByComparingTo("578000");
        assertThat(s.get("cashOut").decimalValue()).isEqualByComparingTo("39000");
    }

    @Test
    void openSellCloseShiftWithAShortage() throws Exception {
        JsonNode s = call(post("/api/cash-shifts/open"), Map.of("openingCash", 500000));
        assertThat(s.get("open").asBoolean()).isTrue();
        // a second register shift is refused
        mvc.perform(post("/api/cash-shifts/open").with(authentication(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"openingCash\":0}")).andExpect(status().isBadRequest());
        paid(PaymentMethod.CASH, 120000);
        paid(PaymentMethod.BANK_TRANSFER, 80000);
        call(post("/api/cash-shifts/current/movements"), Map.of("type", "OUT", "amount", 20000, "reason", "Mua đá"));
        s = call(get("/api/cash-shifts/current"), null);
        // 500.000 + 120.000 − 20.000 = 600.000
        assertThat(s.get("expectedCash").decimalValue()).isEqualByComparingTo("600000");
        assertThat(s.get("transferSales").decimalValue()).isEqualByComparingTo("80000");

        // counted 590.000: 1×500k + 4×20k + 2×5k — a shortage needs a reason
        Map<String, Object> close = Map.of("denominations", Map.of(500000, 1, 20000, 4, 5000, 2), "nextOpeningCash", 300000);
        mvc.perform(post("/api/cash-shifts/current/close").with(authentication(manager)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(close))).andExpect(status().isBadRequest());
        s = call(post("/api/cash-shifts/current/close"), Map.of("denominations", Map.of(500000, 1, 20000, 4, 5000, 2),
                "nextOpeningCash", 300000, "differenceReason", "Thối nhầm", "receivedBy", "Chủ quán"));
        assertThat(s.get("open").asBoolean()).isFalse();
        assertThat(s.get("difference").decimalValue()).isEqualByComparingTo("-10000");
        assertThat(s.get("handoverCash").decimalValue()).isEqualByComparingTo("290000");
        assertThat(call(get("/api/cash-shifts/suggested-opening"), null).get("openingCash").decimalValue()).isEqualByComparingTo("300000");
    }

    @Test
    void scheduledShiftIsCheckedInAndOutAndShowsOnTheTimesheet() throws Exception {
        LocalTime start = LocalTime.now().minusMinutes(30).withSecond(0).withNano(0);
        LocalTime end = start.plusHours(4);
        if (end.isBefore(start)) return; // too close to midnight for this test
        JsonNode shift = call(post("/api/work-shifts"), Map.of("name", "Ca test", "startTime", start.toString(), "endTime", end.toString()));
        String today = LocalDate.now().toString();
        JsonNode added = call(post("/api/shift-assignments"), Map.of("staffIds", List.of(staff.getId()), "dates", List.of(today),
                "shiftId", shift.get("id").asLong(), "position", "Pha chế"));
        long id = added.get(0).get("id").asLong();

        JsonNode mine = call(get("/api/attendance/today"), null);
        assertThat(mine.size()).isEqualTo(1);
        JsonNode in = call(post("/api/attendance/" + id + "/check-in"), null);
        assertThat(in.get("lateMinutes").asLong()).isGreaterThanOrEqualTo(25); // came 30 minutes after the start
        call(post("/api/attendance/" + id + "/check-out"), null);
        // a checked-in shift cannot be removed from the schedule
        mvc.perform(delete("/api/shift-assignments/" + id).with(authentication(manager))).andExpect(status().isBadRequest());

        JsonNode sheet = call(get("/api/timesheets").param("month", YearMonth.now().toString()), null);
        JsonNode row = null;
        for (JsonNode r : sheet.get("rows")) if (r.get("staffId").asLong() == staff.getId()) row = r;
        assertThat(row).isNotNull();
        assertThat(row.get("worked").asInt()).isEqualTo(1);
        assertThat(row.get("lateTimes").asInt()).isEqualTo(1);
        mvc.perform(get("/api/timesheets.xlsx").param("month", YearMonth.now().toString()).with(authentication(manager)))
                .andExpect(status().isOk());
    }

    @Test
    void writeRequestsAreLoggedWithMaskedPasswords() throws Exception {
        String today = LocalDate.now().toString();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ai-do\",\"password\":\"bi-mat-123\"}"));
        call(post("/api/work-shifts"), Map.of("name", "Ca log", "startTime", "06:00", "endTime", "12:00"));
        JsonNode page = call(get("/api/audit-logs").param("from", today).param("to", today).param("keyword", "ca log"), null);
        assertThat(page.get("total").asLong()).isGreaterThanOrEqualTo(1);
        JsonNode entry = page.get("items").get(0);
        assertThat(entry.get("module").asText()).isEqualTo("work-shifts");
        assertThat(entry.get("action").asText()).isEqualTo("CREATE");
        assertThat(entry.get("username").asText()).isEqualTo("lan");

        JsonNode login = call(get("/api/audit-logs").param("from", today).param("to", today).param("module", "auth"), null);
        String detail = login.get("items").get(0).get("detail").asText();
        assertThat(detail).contains("ai-do").doesNotContain("bi-mat-123");
        assertThat(login.get("items").get(0).get("username").asText()).isEqualTo("ai-do");
    }
}
