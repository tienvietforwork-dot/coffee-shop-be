package com.coffeeshop.entity.enums;

/**
 * Only ACTIVE types are issued. INACTIVE = ngừng phát: voucher đã phát vẫn dùng được đến hạn.
 * DRAFT / ENDED are written by app-promotions; app-core treats them like INACTIVE.
 */
public enum VoucherTypeStatus {
    DRAFT, ACTIVE, INACTIVE, ENDED
}
