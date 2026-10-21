package com.coffeeshop.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Purchase document arithmetic shared by phiếu đặt hàng, phiếu nhập and phiếu trả hàng (VND, whole dong). */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    /** thuế GTGT = (tiền hàng − giảm giá) × thuế suất */
    public static BigDecimal vat(BigDecimal goods, BigDecimal discount, BigDecimal vatRate) {
        if (vatRate == null || vatRate.signum() == 0) return BigDecimal.ZERO;
        return vnd(goods.subtract(discount).max(BigDecimal.ZERO).multiply(vatRate).divide(HUNDRED, 2, RoundingMode.HALF_UP));
    }

    /** cần trả = tiền hàng − giảm giá + thuế GTGT + chi phí nhập */
    public static BigDecimal payable(BigDecimal goods, BigDecimal discount, BigDecimal vatRate, BigDecimal extraCost) {
        return vnd(goods.subtract(discount).max(BigDecimal.ZERO).add(vat(goods, discount, vatRate)).add(extraCost));
    }

    public static BigDecimal vnd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }
}
