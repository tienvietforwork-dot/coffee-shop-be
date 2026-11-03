package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tin nhắn chăm sóc khách hàng gửi theo đơn (không phải quảng cáo): one per order event. {@code znsTemplateId}: the
 * template approved by Zalo for the shop's OA (its parameters are filled from the order); {@code smsContent}: the SMS
 * brandname text used when ZNS cannot be delivered (no Zalo / not approved), with {placeholders}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    /** ORDER_CONFIRMED, ORDER_READY, ORDER_DELIVERING, ORDER_COMPLETED, ORDER_CANCELLED */
    @Id
    @Column(length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "zns_template_id", length = 20)
    private String znsTemplateId;
    @Column(name = "sms_content", nullable = false, length = 320)
    private String smsContent;
    /** gửi SMS khi ZNS lỗi */
    @Column(name = "sms_fallback", nullable = false)
    private boolean smsFallback;
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
