package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.VoucherDetail;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class VoucherDetailResponse {

    private Long id;
    private String voucherId;
    private Long orderId;
    private Long customerId;
    private Long incidentId;
    private String code;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public VoucherDetailResponse(VoucherDetail voucherDetail) {
        if (voucherDetail == null) {
            return;
        }

        this.id = voucherDetail.getId();

        if (voucherDetail.getVoucher() != null) {
            this.voucherId = voucherDetail.getVoucher().getName();
        }

        this.orderId = voucherDetail.getOrder();
        this.customerId = voucherDetail.getCustomer();
        this.incidentId = voucherDetail.getIncident();
        this.code = voucherDetail.getCode();

        if (voucherDetail.getStatus() != null) {
            this.status = voucherDetail.getStatus().name();
        }

        this.issuedAt = voucherDetail.getIssuedAt();
        this.expiresAt = voucherDetail.getExpiresAt();
    }
}