package com.coffeeshop.promotions.dto.request;

import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class VoucherDetailRequest {

    private Long voucherId;
    private Long orderId;
    private Long customerId;
    private Long incidentId;
    private String code;
    private VoucherStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
