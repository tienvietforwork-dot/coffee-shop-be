package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.VoucherDetail;
import com.coffeeshop.promotions.entity.enums.IncidentType;
import com.coffeeshop.promotions.entity.enums.IndentSeverity;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class VoucherDetailResponse {

    private Long id;
    private Long orderId;
    private String orderCode;
    private Long customerId;
    private String customerName;
    private Long customerPhone;
    private Long incidentId;
    private IncidentType incidentType;
    private IndentSeverity indentSeverity;
    private String code;
    private VoucherStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public VoucherDetailResponse(VoucherDetail voucherDetail) {


        if (voucherDetail.getOrder() != null){
            this.orderId = voucherDetail.getOrder().getId();
            this.orderCode = voucherDetail.getOrder().getCode();
        }
        if (voucherDetail.getCustomer() != null) {
            this.customerId = voucherDetail.getCustomer().getId();
            this.customerName = voucherDetail.getCustomer().getFullName();
            this.customerPhone = voucherDetail.getCustomer().getPhone();
        }
        if (voucherDetail.getIncident() != null){
            this.incidentId = voucherDetail.getIncident().getId();
            this.incidentType = voucherDetail.getIncident().getType();
            this.indentSeverity = voucherDetail.getIncident().getSeverity();
        }
        this.code = voucherDetail.getCode();
        this.status = voucherDetail.getStatus();
        this.issuedAt = voucherDetail.getIssuedAt();
        this.expiresAt = voucherDetail.getExpiresAt();

    }
}