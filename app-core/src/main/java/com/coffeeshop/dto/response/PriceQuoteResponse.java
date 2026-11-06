package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** {@code deliveryDistanceKm} / {@code deliveryTier}: quãng đường quán → địa chỉ giao và bậc phí áp dụng; {@code deliveryOption}: option giá của khung giờ đang tính (null = option mặc định); {@code deliveryWeather} / {@code weatherSurcharge}: thời tiết xấu và phụ phí đã cộng vào phí ship. */
public record PriceQuoteResponse(List<Line> lines, BigDecimal subtotal, String promotionName,
                                 BigDecimal promotionDiscount, String voucherCode, BigDecimal voucherDiscount,
                                 String voucherMessage, BigDecimal shippingFee, BigDecimal total,
                                 List<String> unavailableItems, BigDecimal deliveryDistanceKm, String deliveryTier,
                                 String deliveryOption, String deliveryWeather, BigDecimal weatherSurcharge) {

    public record Line(Long coffeeId, String coffeeName, Integer quantity, BigDecimal listPrice,
                       BigDecimal unitPrice, String promotionName, BigDecimal lineTotal, String note) {
    }

    public PriceQuoteResponse withDelivery(BigDecimal km, String tier, String peak, String weather, BigDecimal surcharge) {
        return new PriceQuoteResponse(lines, subtotal, promotionName, promotionDiscount, voucherCode, voucherDiscount,
                voucherMessage, shippingFee, total, unavailableItems, km, tier, peak, weather, surcharge);
    }
}
