package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Các trường phản hồi cần cập nhật; trường bỏ qua sẽ giữ nguyên")
public class FeedbackUpdateRequest {
    @Positive
    private Long staffId;
    @Pattern(regexp = "COMPLAINT|SUGGESTION|PRAISE")
    private String category;
    @Size(max = 50)
    private String topic;

    @Size(max = 10)
    private List<@Size(max = 100) String> topics;
    @Pattern(regexp = "PRODUCT|SERVICE|DELIVERY|PAYMENT|OTHER")
    private String issueType;
    @Pattern(regexp = "POSITIVE|NEUTRAL|NEGATIVE")
    private String sentiment;
    @Pattern(regexp = "NEW|IN_PROGRESS|ESCALATED|RESOLVED|CLOSED")
    private String status;
    private Boolean escalated;
    @Size(max = 10000)
    private String resolution;
    @Pattern(regexp = "SATISFIED|UNSATISFIED")
    private String satisfaction;
    @Size(max = 10000)
    private String content;
    @Min(1)
    @Max(5)
    private Integer rating;
}
