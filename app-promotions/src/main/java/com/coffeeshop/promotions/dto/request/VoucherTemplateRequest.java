package com.coffeeshop.promotions.dto.request;

import com.coffeeshop.promotions.entity.enums.VoucherTemplateCustomerType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateOrderType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplatesStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTemplateRequest {

    private String name;
    private Long voucherId;
    private VoucherTemplateCustomerType customerType;
    private Long customerGroupId;
    private VoucherTemplateOrderType orderType;
    private Long completedOrderCount;
    private BigDecimal minOrderAmount;
    private Long requiredPoints;
    private Long maxQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VoucherTemplatesStatus status;
}