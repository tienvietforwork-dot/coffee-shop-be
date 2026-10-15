package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Kết quả và mức độ hài lòng khi đóng phản hồi")
public class FeedbackCloseRequest {
    @Size(max = 10000)
    private String resolution;

    @Pattern(regexp = "SATISFIED|UNSATISFIED")
    private String satisfaction;
}
