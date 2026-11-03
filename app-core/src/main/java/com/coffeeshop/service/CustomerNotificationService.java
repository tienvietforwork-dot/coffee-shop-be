package com.coffeeshop.service;

import com.coffeeshop.entity.NotificationMessage;
import com.coffeeshop.entity.NotificationTemplate;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderChannel;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.esms.EsmsGateway;
import com.coffeeshop.repository.NotificationMessageRepository;
import com.coffeeshop.repository.NotificationTemplateRepository;
import com.coffeeshop.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thông báo đơn hàng cho khách qua Zalo ZNS / SMS brandname (tin chăm sóc khách hàng, không quảng cáo): xác nhận đơn,
 * đồ uống sẵn sàng (mang về), đang giao, hoàn tất, hủy. Only QR / online orders with a phone; counter customers are there.
 * Messages are queued with the order and sent by a job (retries, ZNS → SMS fallback); each event goes once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationTemplateRepository templateRepository;
    private final NotificationMessageRepository messageRepository;
    private final EsmsGateway gateway;
    private final ObjectMapper json;

    @Value("${app.public-site-url:http://localhost:5173}")
    private String siteUrl;

    /** called on every order status change */
    @Transactional
    public void onOrderStatus(Order order, OrderStatus to) {
        if (order.getChannel() == OrderChannel.COUNTER || order.getCustomer() == null) return;
        String code = switch (to) {
            case PREPARING -> "ORDER_CONFIRMED";
            case READY -> order.getOrderType() == OrderType.TAKE_AWAY ? "ORDER_READY" : null;
            case DELIVERING -> "ORDER_DELIVERING";
            case COMPLETED -> "ORDER_COMPLETED";
            case CANCELLED, REJECTED -> "ORDER_CANCELLED";
            default -> null;
        };
        if (code == null) return;
        NotificationTemplate t = templateRepository.findById(code).orElse(null);
        if (t == null || !t.isEnabled()) return;
        String requestId = order.getOrderCode() + "-" + code;
        if (messageRepository.existsByRequestId(requestId)) return;
        Map<String, String> params = params(order, to);
        messageRepository.save(NotificationMessage.builder().order(order).templateCode(code).phone(intl(order.getCustomer().getPhone()))
                .status("PENDING").requestId(requestId).params(write(params)).content(render(t.getSmsContent(), params))
                .createdAt(LocalDateTime.now()).build());
    }

    @Scheduled(fixedDelayString = "${app.notify.send-ms:10000}", initialDelay = 20_000)
    @Transactional
    public int sendQueued() {
        int sent = 0;
        for (NotificationMessage m : messageRepository.findQueued(MAX_ATTEMPTS)) {
            NotificationTemplate t = templateRepository.findById(m.getTemplateCode()).orElse(null);
            if (t == null || !t.isEnabled()) {
                m.setStatus("FAILED");
                m.setError("Mẫu tin đã tắt");
                continue;
            }
            EsmsGateway.Result r = gateway.send(m.getPhone(), t.getZnsTemplateId(), read(m.getParams()), m.getContent(), t.isSmsFallback(),
                    m.getRequestId() + (m.getAttempts() > 0 ? "-" + m.getAttempts() : ""));
            m.setAttempts(m.getAttempts() + 1);
            if (r.accepted()) {
                m.setStatus("SENT");
                m.setChannel(r.channel());
                m.setProviderRef(r.providerRef());
                m.setSentAt(LocalDateTime.now());
                m.setError(null);
                sent++;
            } else {
                m.setError(r.error() == null ? "Lỗi gửi" : r.error().substring(0, Math.min(500, r.error().length())));
                if (m.getAttempts() >= MAX_ATTEMPTS) m.setStatus("FAILED");
            }
        }
        return sent;
    }

    /** delivery report from eSMS */
    @Transactional
    public boolean delivered(String requestId, boolean ok, String channel, String token) {
        if (token == null || !token.equals(gateway.props().getCallbackToken())) return false;
        NotificationMessage m = messageRepository.findByRequestId(requestId).orElse(null);
        if (m == null) return false;
        m.setStatus(ok ? "DELIVERED" : "FAILED");
        if (channel != null) m.setChannel(channel);
        return true;
    }

    // ---- quản lý -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationTemplate> templates() {
        return templateRepository.findAll();
    }

    @Transactional
    public NotificationTemplate updateTemplate(String code, Boolean enabled, String znsTemplateId, String smsContent, Boolean smsFallback) {
        NotificationTemplate t = templateRepository.findById(code).orElseThrow(() -> new ResourceNotFoundException("Không có mẫu tin: " + code));
        if (smsContent != null) {
            if (smsContent.isBlank()) throw new BadRequestException("Nội dung SMS không được trống");
            // SMS brandname: không dấu, tối đa 2 tin (≈ 320 ký tự)
            if (smsContent.length() > 320) throw new BadRequestException("Nội dung SMS tối đa 320 ký tự");
            t.setSmsContent(smsContent.trim());
        }
        if (enabled != null) t.setEnabled(enabled);
        if (znsTemplateId != null) t.setZnsTemplateId(znsTemplateId.isBlank() ? null : znsTemplateId.trim());
        if (smsFallback != null) t.setSmsFallback(smsFallback);
        t.setUpdatedBy(CurrentUser.username());
        return t;
    }

    @Transactional(readOnly = true)
    public List<NotificationMessage> search(LocalDate from, LocalDate to, String status, String phone) {
        String p = phone == null || phone.isBlank() ? null : "%" + phone.trim().replaceFirst("^0", "") + "%";
        return messageRepository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), status == null || status.isBlank() ? null : status, p);
    }

    /** Gửi lại a failed message */
    @Transactional
    public NotificationMessage resend(Long id) {
        NotificationMessage m = messageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin: " + id));
        if (!"FAILED".equals(m.getStatus())) throw new BadRequestException("Chỉ gửi lại tin lỗi");
        m.setStatus("PENDING");
        m.setAttempts(0);
        m.setError(null);
        return m;
    }

    // ---- helpers ---------------------------------------------------------------------

    private Map<String, String> params(Order o, OrderStatus to) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("customer_name", o.getCustomer().getFullName() == null ? "Quý khách" : o.getCustomer().getFullName());
        p.put("order_code", o.getOrderCode());
        p.put("amount", NumberFormat.getInstance(new Locale("vi", "VN")).format(o.getTotalAmount()) + "d");
        p.put("status", switch (to) {
            case PREPARING -> "Dang pha che";
            case READY -> "San sang";
            case DELIVERING -> "Dang giao";
            case COMPLETED -> "Hoan tat";
            default -> "Da huy";
        });
        p.put("link", siteUrl + "/order/" + o.getOrderCode());
        if (o.getShipment() != null && o.getShipment().getDriverName() != null) {
            p.put("driver", o.getShipment().getDriverName() + " " + (o.getShipment().getDriverPhone() == null ? "" : o.getShipment().getDriverPhone()));
        } else {
            p.put("driver", "");
        }
        p.put("reason", o.getCancelReason() == null ? "" : o.getCancelReason());
        return p;
    }

    /** {customer_name}, {order_code}… replaced; SMS brandname text goes without accents */
    private static String render(String template, Map<String, String> params) {
        String s = template;
        for (var e : params.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D');
    }

    /** 0912… → 84912… as eSMS / Zalo expect */
    private static String intl(String phone) {
        String p = phone.replaceAll("\\D", "");
        return p.startsWith("0") ? "84" + p.substring(1) : p;
    }

    private String write(Map<String, String> m) {
        try {
            return json.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> read(String s) {
        try {
            return s == null ? Map.of() : json.readValue(s, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
