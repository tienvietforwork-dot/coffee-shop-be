package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PriceQuoteResponse(List<Line> lines, BigDecimal subtotal, String promotionName,
                                 BigDecimal promotionDiscount, String voucherCode, BigDecimal voucherDiscount,
                                 String voucherMessage, BigDecimal shippingFee, BigDecimal total,
                                 List<String> unavailableItems) {

    public record Line(Long coffeeId, String coffeeName, Integer quantity, BigDecimal listPrice,
                       BigDecimal unitPrice, String promotionName, BigDecimal lineTotal, String note) {
    }
}
