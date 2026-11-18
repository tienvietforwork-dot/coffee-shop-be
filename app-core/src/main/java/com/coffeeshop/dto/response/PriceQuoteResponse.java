package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code deliveryZone}: khu vực giao ("Phường …, Thành phố …") quyết định phí ship; {@code deliveryDistanceKm}: quãng đường
 * quán → địa chỉ (chỉ để hiển thị, có thể null); {@code deliveryNote}: ghi chú phí (miễn phí cho khách VIP…);
 * {@code prepMinutes}: tổng thời gian pha chế các món (phút); {@code deliveryMinutes}: thời gian giao theo cấu hình khu vực
 * (phút) — cộng lại để ước tính giờ giao tới.
 */
public record PriceQuoteResponse(List<Line> lines, BigDecimal subtotal, String promotionName,
                                 BigDecimal promotionDiscount, String voucherCode, BigDecimal voucherDiscount,
                                 String voucherMessage, BigDecimal shippingFee, BigDecimal total,
                                 List<String> unavailableItems, BigDecimal deliveryDistanceKm, String deliveryZone,
                                 String deliveryNote, Integer prepMinutes, Integer deliveryMinutes) {

    public record Line(Long coffeeId, String coffeeName, Integer quantity, BigDecimal listPrice,
                       BigDecimal unitPrice, String promotionName, BigDecimal lineTotal, String note) {
    }

    public PriceQuoteResponse withDelivery(BigDecimal km, String zone, String note, Integer deliveryMinutes) {
        return new PriceQuoteResponse(lines, subtotal, promotionName, promotionDiscount, voucherCode, voucherDiscount,
                voucherMessage, shippingFee, total, unavailableItems, km, zone, note, prepMinutes, deliveryMinutes);
    }

    public PriceQuoteResponse withPrepMinutes(Integer minutes) {
        return new PriceQuoteResponse(lines, subtotal, promotionName, promotionDiscount, voucherCode, voucherDiscount,
                voucherMessage, shippingFee, total, unavailableItems, deliveryDistanceKm, deliveryZone, deliveryNote, minutes, deliveryMinutes);
    }
}
