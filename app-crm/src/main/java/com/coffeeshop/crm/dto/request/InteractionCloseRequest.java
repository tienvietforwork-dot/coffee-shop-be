package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Kết quả cuối cùng của hoạt động chăm sóc khách hàng")
public class InteractionCloseRequest {
    @Size(max = 10000)
    private String outcome;
}
