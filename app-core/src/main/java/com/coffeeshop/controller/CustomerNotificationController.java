package com.coffeeshop.controller;

import com.coffeeshop.entity.NotificationMessage;
import com.coffeeshop.entity.NotificationTemplate;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CustomerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Tin nhắn ZNS / SMS gửi khách theo đơn: mẫu tin (bật / tắt, nội dung), nhật ký gửi, gửi lại; callback của eSMS. */
@RestController
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final CustomerNotificationService service;

    public record TemplateRequest(Boolean enabled, String znsTemplateId, String smsContent, Boolean smsFallback) {
    }

    /** one row of the send log */
    public record Message(Long id, String orderCode, String templateCode, String phone, String status, String channel,
                          String content, int attempts, String providerRef, String error, LocalDateTime sentAt,
                          LocalDateTime createdAt) {
        static Message from(NotificationMessage m) {
            return new Message(m.getId(), m.getOrder() == null ? null : m.getOrder().getOrderCode(), m.getTemplateCode(),
                    m.getPhone(), m.getStatus(), m.getChannel(), m.getContent(), m.getAttempts(), m.getProviderRef(),
                    m.getError(), m.getSentAt(), m.getCreatedAt());
        }
    }

    @GetMapping("/api/notifications/templates")
    @PreAuthorize(Perm.CAN_NOTIFICATIONS)
    public List<NotificationTemplate> templates() {
        return service.templates();
    }

    @PutMapping("/api/notifications/templates/{code}")
    @PreAuthorize(Perm.CAN_NOTIFICATIONS)
    public NotificationTemplate update(@PathVariable String code, @RequestBody TemplateRequest r) {
        return service.updateTemplate(code, r.enabled(), r.znsTemplateId(), r.smsContent(), r.smsFallback());
    }

    @GetMapping("/api/notifications/messages")
    @PreAuthorize(Perm.CAN_NOTIFICATIONS)
    public List<Message> messages(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String phone) {
        return service.search(from, to, status, phone).stream().map(Message::from).toList();
    }

    @PostMapping("/api/notifications/messages/{id}/resend")
    @PreAuthorize(Perm.CAN_NOTIFICATIONS)
    public Message resend(@PathVariable Long id) {
        return Message.from(service.resend(id));
    }

    /** eSMS delivery report: {RequestId, Status (1 = delivered), Channel} */
    @PostMapping("/api/public/notifications/esms/callback")
    public Map<String, String> callback(@RequestParam(required = false) String token, @RequestBody Map<String, Object> body) {
        boolean ok = "1".equals(String.valueOf(body.get("Status"))) || "DELIVERED".equalsIgnoreCase(String.valueOf(body.get("Status")));
        boolean found = service.delivered(String.valueOf(body.get("RequestId")), ok,
                body.get("Channel") == null ? null : String.valueOf(body.get("Channel")).toUpperCase(), token);
        return Map.of("result", found ? "ok" : "ignored");
    }
}
