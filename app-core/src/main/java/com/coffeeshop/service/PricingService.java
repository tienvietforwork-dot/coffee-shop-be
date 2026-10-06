package com.coffeeshop.service;

import com.coffeeshop.dto.response.PriceQuoteResponse;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Promotion;
import com.coffeeshop.entity.PromotionItem;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.VoucherStatus;
import com.coffeeshop.repository.PromotionRepository;
import com.coffeeshop.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes order prices: running "đợt giảm giá" (per-coffee and whole-order) and vouchers.
 * Rules:
 *  - coffee promotion: the lowest promo price among running promotions that still have quota;
 *  - order promotion: the running ORDER promotion giving the biggest discount whose minimum is met;
 *  - voucher: discount_value once the subtotal reaches min_order_value, on top of the order promotion,
 *    capped at what is left to pay;
 *  - delivery orders pay a flat shipping fee (app.shipping.fee).
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;

    @Value("${app.shipping.fee:15000}")
    private BigDecimal shippingFee;

    public record CoffeePrice(BigDecimal price, PromotionItem item) {
    }

    public record InputLine(Coffee coffee, int quantity, String note) {
    }

    public record PricedLine(Coffee coffee, int quantity, String note, BigDecimal unitPrice, PromotionItem promoItem) {
        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record Quote(List<PricedLine> lines, BigDecimal subtotal, Promotion orderPromotion,
                        BigDecimal promotionDiscount, Voucher voucher, String voucherCode,
                        BigDecimal voucherDiscount, String voucherMessage, BigDecimal shippingFee,
                        BigDecimal total, List<String> unavailable) {

        public BigDecimal discountAmount() {
            return promotionDiscount.add(voucherDiscount);
        }

        /** Promotion to record on the order: the order-level one, else the first coffee-level one used. */
        public Promotion appliedPromotion() {
            if (orderPromotion != null) return orderPromotion;
            return lines.stream().filter(l -> l.promoItem() != null).map(l -> l.promoItem().getPromotion())
                    .findFirst().orElse(null);
        }

        public PriceQuoteResponse toResponse() {
            return new PriceQuoteResponse(
                    lines.stream().map(l -> new PriceQuoteResponse.Line(l.coffee().getId(), l.coffee().getName(),
                            l.quantity(), l.coffee().getPrice(), l.unitPrice(),
                            l.promoItem() == null ? null : l.promoItem().getPromotion().getName(),
                            l.lineTotal(), l.note())).toList(),
                    subtotal, orderPromotion == null ? null : orderPromotion.getName(), promotionDiscount,
                    voucher == null ? null : voucher.getCode(), voucherDiscount, voucherMessage, shippingFee, total,
                    unavailable);
        }
    }

    public Map<Long, List<PromotionItem>> runningCoffeePromotions(LocalDateTime now) {
        return promotionRepository.findRunningCoffeeItems(now).stream()
                .collect(Collectors.groupingBy(i -> i.getCoffee().getId()));
    }

    public CoffeePrice bestCoffeePrice(Coffee coffee, int quantity, List<PromotionItem> items) {
        CoffeePrice best = null;
        for (PromotionItem item : items) {
            if (!item.hasRemaining(quantity)) continue;
            BigDecimal price = promoPrice(coffee, item);
            if (price.compareTo(coffee.getPrice()) < 0 && (best == null || price.compareTo(best.price()) < 0)) {
                best = new CoffeePrice(price, item);
            }
        }
        return best;
    }

    public BigDecimal currentUnitPrice(Coffee coffee, LocalDateTime now) {
        var best = bestCoffeePrice(coffee, 1, runningCoffeePromotions(now).getOrDefault(coffee.getId(), List.of()));
        return best == null ? coffee.getPrice() : best.price();
    }

    public Quote quote(List<InputLine> input, OrderType orderType, String customerPhone, String voucherCode,
                       LocalDateTime now) {
        Map<Long, List<PromotionItem>> coffeePromos = runningCoffeePromotions(now);
        Map<Long, Integer> promoUsage = new HashMap<>();
        List<PricedLine> lines = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (InputLine in : input) {
            Coffee coffee = in.coffee();
            if (coffee.getStatus() != CoffeeStatus.AVAILABLE) unavailable.add(coffee.getName());
            List<PromotionItem> candidates = coffeePromos.getOrDefault(coffee.getId(), List.of()).stream()
                    .filter(i -> i.hasRemaining(in.quantity() + promoUsage.getOrDefault(i.getId(), 0)))
                    .toList();
            CoffeePrice best = bestCoffeePrice(coffee, in.quantity(), candidates);
            if (best != null) promoUsage.merge(best.item().getId(), in.quantity(), Integer::sum);
            PricedLine line = new PricedLine(coffee, in.quantity(), in.note(),
                    best == null ? coffee.getPrice() : best.price(), best == null ? null : best.item());
            lines.add(line);
            subtotal = subtotal.add(line.lineTotal());
        }

        Promotion orderPromotion = null;
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        for (Promotion p : promotionRepository.findRunningOrderPromotions(now)) {
            if (p.getDiscountPercent() == null) continue;
            if (p.getMinOrderAmount() != null && subtotal.compareTo(p.getMinOrderAmount()) < 0) continue;
            BigDecimal discount = vnd(subtotal.multiply(p.getDiscountPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP));
            if (discount.compareTo(promotionDiscount) > 0) {
                promotionDiscount = discount;
                orderPromotion = p;
            }
        }

        BigDecimal afterPromotion = subtotal.subtract(promotionDiscount);
        Voucher voucher = null;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String voucherMessage = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherRepository.findByCodeIgnoreCase(voucherCode.trim()).orElse(null);
            voucherMessage = validateVoucher(voucher, customerPhone, subtotal, now);
            if (voucherMessage == null) {
                voucherDiscount = voucher.getDiscountValue().min(afterPromotion);
            } else {
                voucher = null;
            }
        }

        BigDecimal fee = orderType == OrderType.DELIVERY ? shippingFee : BigDecimal.ZERO;
        BigDecimal total = afterPromotion.subtract(voucherDiscount).add(fee);
        return new Quote(lines, subtotal, orderPromotion, promotionDiscount, voucher,
                voucherCode == null ? null : voucherCode.trim(), voucherDiscount, voucherMessage, fee, total,
                unavailable);
    }

    /** The coffee promotion item that produced {@code unitPrice}, used to give back quota on cancellation. */
    public Optional<PromotionItem> findPromotionItemUsed(Coffee coffee, BigDecimal unitPrice, LocalDateTime at) {
        if (unitPrice.compareTo(coffee.getPrice()) >= 0) return Optional.empty();
        return runningCoffeePromotions(at).getOrDefault(coffee.getId(), List.of()).stream()
                .filter(i -> i.getSoldQuantity() > 0 && promoPrice(coffee, i).compareTo(unitPrice) == 0)
                .findFirst();
    }

    /** Returns null when the voucher can be used, otherwise the reason it is rejected. */
    private String validateVoucher(Voucher voucher, String customerPhone, BigDecimal subtotal, LocalDateTime now) {
        if (voucher == null) return "Mã voucher không tồn tại";
        if (voucher.getStatus() != VoucherStatus.ISSUED) return "Voucher đã được sử dụng hoặc không còn hiệu lực";
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(now)) return "Voucher đã hết hạn";
        if (voucher.getCustomer() != null
                && (customerPhone == null || !voucher.getCustomer().getPhone().equals(customerPhone.trim()))) {
            return "Voucher không thuộc về số điện thoại này";
        }
        BigDecimal min = voucher.getMinOrderValue();
        if (min != null && subtotal.compareTo(min) < 0) {
            return "Đơn hàng chưa đạt giá trị tối thiểu " + min.stripTrailingZeros().toPlainString() + "đ";
        }
        return null;
    }

    private BigDecimal promoPrice(Coffee coffee, PromotionItem item) {
        BigDecimal price = coffee.getPrice();
        if (item.getSalePrice() != null) {
            price = item.getSalePrice();
        } else if (item.getCustomDiscount() != null) {
            price = price.subtract(item.getCustomDiscount());
        } else if (item.getPromotion().getDiscountPercent() != null) {
            price = price.multiply(HUNDRED.subtract(item.getPromotion().getDiscountPercent()))
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return vnd(price.max(BigDecimal.ZERO));
    }

    /** VND has no minor unit: round to whole dong. */
    private static BigDecimal vnd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }
}
