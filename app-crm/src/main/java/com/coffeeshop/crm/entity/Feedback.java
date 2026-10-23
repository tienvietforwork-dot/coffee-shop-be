package com.coffeeshop.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Schema(title = "Phản hồi khách hàng", description = "Phân loại, cảm xúc, nhiều chủ đề và kết quả xử lý")
@Table(name = "feedbacks")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE feedbacks SET del_flag = true WHERE id = ?")
@Getter @Setter
public class Feedback extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "topic", length = 50)
    private String topic;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feedback_topics", joinColumns = @JoinColumn(name = "feedback_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"feedback_id", "topic"}))
    @Column(name = "topic", nullable = false, length = 100)
    private Set<String> topics = new LinkedHashSet<>();

    @Column(name = "issue_type", length = 20)
    private String issueType;

    @Column(name = "category", length = 30)
    private String category; // COMPLAINT, SUGGESTION, PRAISE

    @Column(name = "sentiment", length = 20)
    private String sentiment; // POSITIVE, NEUTRAL, NEGATIVE

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "NEW"; // NEW, IN_PROGRESS, RESOLVED, etc.

    @Column(name = "escalated", nullable = false)
    private Boolean escalated = false;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "satisfaction", length = 20)
    private String satisfaction; // SATISFIED, UNSATISFIED

    @Column(name = "rating")
    private Short rating;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "approval_status", length = 20, nullable = false)
    private String approvalStatus = "NOT_REQUIRED";

    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
