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
@Schema(title = "Lịch sử trả lời khách hàng", description = "Một nội dung phản hồi được ghi nhận để gửi cho khách")
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

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

}
