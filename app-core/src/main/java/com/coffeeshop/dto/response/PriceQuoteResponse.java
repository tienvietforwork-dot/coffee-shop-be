package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code deliveryZone}: khu vực giao ("Phường …, Thành phố …") quyết định phí ship; {@code deliveryDistanceKm}: quãng đường
 * quán → địa chỉ (chỉ để ước tính giờ giao, có thể null); {@code deliveryNote}: ghi chú phí (miễn phí cho khách VIP…);
 * {@code prepMinutes}: tổng thời gian pha chế các món (phút) để ước tính giờ xong / giờ giao tới.
 */
public record PriceQuoteResponse(List<Line> lines, BigDecimal subtotal, String promotionName,
                                 BigDecimal promotionDiscount, String voucherCode, BigDecimal voucherDiscount,
                                 String voucherMessage, BigDecimal shippingFee, BigDecimal total,
                                 List<String> unavailableItems, BigDecimal deliveryDistanceKm, String deliveryZone,
                                 String deliveryNote, Integer prepMinutes) {

    public record Line(Long coffeeId, String coffeeName, Integer quantity, BigDecimal listPrice,
                       BigDecimal unitPrice, String promotionName, BigDecimal lineTotal, String note) {
    }

    public PriceQuoteResponse withDelivery(BigDecimal km, String zone, String note) {
        return new PriceQuoteResponse(lines, subtotal, promotionName, promotionDiscount, voucherCode, voucherDiscount,
                voucherMessage, shippingFee, total, unavailableItems, km, zone, note, prepMinutes);
    }

    public PriceQuoteResponse withPrepMinutes(Integer minutes) {
        return new PriceQuoteResponse(lines, subtotal, promotionName, promotionDiscount, voucherCode, voucherDiscount,
                voucherMessage, shippingFee, total, unavailableItems, deliveryDistanceKm, deliveryZone, deliveryNote, minutes);
    }
}
