package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Nội dung phản hồi; sentiment tự phân loại nếu không truyền, có thể gắn tối đa 10 chủ đề")
public class FeedbackRequest {
    @NotNull
    @Positive
    private Long customerId;
    @Positive
    private Long staffId;
    @Size(max = 50)
    private String topic;

    @Size(max = 10)
    private List<@Size(max = 100) String> topics;
    @Pattern(regexp = "PRODUCT|SERVICE|DELIVERY|PAYMENT|OTHER")
    private String issueType;
    @Pattern(regexp = "COMPLAINT|SUGGESTION|PRAISE")
    private String category;
    @Pattern(regexp = "POSITIVE|NEUTRAL|NEGATIVE")
    private String sentiment;
    @NotBlank
    @Size(max = 10000)
    private String content;

    @Min(1)
    @Max(5)
    private Integer rating;
}
