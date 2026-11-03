package com.coffeeshop.promotions.dto.request;

import com.coffeeshop.promotions.entity.Voucher;
import com.coffeeshop.promotions.entity.enums.VoucherType;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class VoucherRequest {

    private String name;
    private VoucherType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer validDate;
    private VoucherTypeStatus status;
}
