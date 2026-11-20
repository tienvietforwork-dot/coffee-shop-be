package com.coffeeshop.entity.enums;

/**
 * vouchers.discount_type, written by app-promotions. PERCENTAGE: discount_value is a percent of the amount;
 * FIXED_AMOUNT (and older rows where the column is null): discount_value is in đồng.
 */
public enum VoucherDiscountType {
    PERCENTAGE, FIXED_AMOUNT
}
