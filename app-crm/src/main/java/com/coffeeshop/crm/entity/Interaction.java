package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Entity
@Schema(title = "Lịch sử tương tác", description = "Kênh liên hệ, thời điểm, lịch nhắc và kết quả chăm sóc")
@Table(name = "interactions")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE interactions SET del_flag = true WHERE id = ?")
@Getter @Setter
public class Interaction extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "channel", length = 20)
    private String channel; // CALL, CHAT, EMAIL, IN_STORE, NOTE

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN"; // OPEN, FOLLOW_UP, CLOSED

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    @Column(name = "visible_to_customer", nullable = false)
    private Boolean visibleToCustomer = false;
}
