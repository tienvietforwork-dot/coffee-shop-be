package com.coffeeshop.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Schema(title = "Tin nhắn trong hồ sơ phản hồi", description = "Trao đổi giữa khách hàng và nhân viên; có lưu thời điểm khách đọc trả lời")
@Table(name = "feedback_responses")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE feedback_responses SET del_flag = true WHERE id = ?")
@Getter
@Setter
public class FeedbackResponse extends BaseEntity {
    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;

    @Column(name = "staff_id")
    private Long staffId;

    /** Author of this conversation message: STAFF or CUSTOMER. */
    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType = "STAFF";

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

}
