package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.Voucher;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.entity.enums.VoucherType;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class VoucherResponse {

  private Long id;
  private List<VoucherDetailResponse> items;
  private String name;
  private VoucherType discountType;
  private BigDecimal discountValue;
  private BigDecimal minOrderValue;
  private Integer quantity;
  private Integer validDays;
  private VoucherTypeStatus status;

  public VoucherResponse(Voucher voucher){
      this.id = voucher.getId();
      this.name = voucher.getName();
      this.discountType = voucher.getDiscountType();
      this.discountValue = voucher.getDiscountValue();
      this.minOrderValue = voucher.getMinOrderValue();
      this.quantity = voucher.getQuantity();
      this.validDays = voucher.getValidDays();
      this.status = voucher.getStatus();

      if (voucher.getVoucherDetails() != null){
          this.items = voucher.getVoucherDetails().stream().map(VoucherDetailResponse::new).collect(Collectors.toList());
      }
  }

}
