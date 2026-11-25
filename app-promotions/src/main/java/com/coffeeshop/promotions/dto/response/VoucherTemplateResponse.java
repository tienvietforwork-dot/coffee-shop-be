package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.VoucherTemplates;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateCustomerType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateOrderType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplatesStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTemplateResponse {

    private Long id;
    private String name;
    private Long voucherId;
    private String voucherName;
    private VoucherTemplateCustomerType customerType;
    private Long customerGroupId;
    private String customerGroupName;
    private VoucherTemplateOrderType orderType;
    private Long completedOrderCount;
    private BigDecimal minOrderAmount;
    private Long requiredPoints;
    private Long maxQuantity;
    private Long redeemedQuantity;
    private Integer voucherQuantity;
    private Integer voucherMaxQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VoucherTemplatesStatus status;

    public VoucherTemplateResponse(VoucherTemplates entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        if (entity.getVoucher() != null) {
            this.voucherId = entity.getVoucher().getId();
            this.voucherName = entity.getVoucher().getName();
            this.voucherQuantity = entity.getVoucher().getQuantity();
            this.voucherMaxQuantity = entity.getVoucher().getMaxQuantity();
        }
        this.customerType = entity.getCustomerType();
        if (entity.getCustomerGroup() != null) {
            this.customerGroupId = entity.getCustomerGroup().getId();
            this.customerGroupName = entity.getCustomerGroup().getName();
        }
        this.orderType = entity.getOrderType();
        this.completedOrderCount = entity.getCompletedOrderCount();
        this.minOrderAmount = entity.getMinOrderAmount();
        this.requiredPoints = entity.getRequiredPoints();
        this.maxQuantity = entity.getMaxQuantity();
        this.startDate = entity.getStartDate();
        this.endDate = entity.getEndDate();
        this.status = entity.getStatus();
    }
}