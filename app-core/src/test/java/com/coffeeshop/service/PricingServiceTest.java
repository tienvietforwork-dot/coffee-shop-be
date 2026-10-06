package com.coffeeshop.service;

import com.coffeeshop.entity.Category;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.Promotion;
import com.coffeeshop.entity.PromotionItem;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.PromotionScope;
import com.coffeeshop.entity.enums.PromotionStatus;
import com.coffeeshop.entity.enums.VoucherStatus;
import com.coffeeshop.repository.PromotionRepository;
import com.coffeeshop.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private VoucherRepository voucherRepository;

    private PricingService pricingService;
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 6, 10, 0);
    private Coffee latte;
    private Coffee coldBrew;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(promotionRepository, voucherRepository);
        ReflectionTestUtils.setField(pricingService, "shippingFee", new BigDecimal("15000"));
        Category category = Category.builder().id(1L).name("Espresso").build();
        latte = Coffee.builder().id(1L).category(category).name("Latte").price(new BigDecimal("50000")).build();
        coldBrew = Coffee.builder().id(2L).category(category).name("Cold Brew").price(new BigDecimal("45000")).build();
        lenient().when(promotionRepository.findRunningCoffeeItems(any())).thenReturn(List.of());
        lenient().when(promotionRepository.findRunningOrderPromotions(any())).thenReturn(List.of());
    }

    @Test
    void coffeePromotionUsesSalePriceWhileQuotaLasts() {
        Promotion promo = promotion(PromotionScope.COFFEE, null, null);
        PromotionItem item = PromotionItem.builder().id(9L).promotion(promo).coffee(coldBrew)
                .salePrice(new BigDecimal("36000")).maxQuantity(3).soldQuantity(1).build();
        when(promotionRepository.findRunningCoffeeItems(any())).thenReturn(List.of(item));

        var quote = pricingService.quote(List.of(
                new PricingService.InputLine(coldBrew, 2, null),   // fits the remaining quota of 2
                new PricingService.InputLine(coldBrew, 1, "ít đá")), // quota used up -> list price
                OrderType.TAKE_AWAY, null, null, now);

        assertThat(quote.lines().get(0).unitPrice()).isEqualByComparingTo("36000");
        assertThat(quote.lines().get(1).unitPrice()).isEqualByComparingTo("45000");
        assertThat(quote.subtotal()).isEqualByComparingTo("117000");
    }

    @Test
    void orderPromotionNeedsMinimumAndVoucherIsAppliedOnTop() {
        when(promotionRepository.findRunningOrderPromotions(any()))
                .thenReturn(List.of(promotion(PromotionScope.ORDER, "10", "150000")));
        Customer owner = Customer.builder().id(5L).phone("0912345678").build();
        Voucher voucher = Voucher.builder().id(1L).code("NAM50K").customer(owner).discountValue(new BigDecimal("50000"))
                .minOrderValue(new BigDecimal("100000")).status(VoucherStatus.ISSUED).expiresAt(now.plusDays(1)).build();
        when(voucherRepository.findByCodeIgnoreCase("NAM50K")).thenReturn(Optional.of(voucher));

        var quote = pricingService.quote(List.of(new PricingService.InputLine(latte, 4, null)),
                OrderType.DELIVERY, "0912345678", "NAM50K", now);

        assertThat(quote.subtotal()).isEqualByComparingTo("200000");
        assertThat(quote.promotionDiscount()).isEqualByComparingTo("20000");
        assertThat(quote.voucherDiscount()).isEqualByComparingTo("50000");
        assertThat(quote.shippingFee()).isEqualByComparingTo("15000");
        assertThat(quote.total()).isEqualByComparingTo("145000");
        assertThat(quote.voucherMessage()).isNull();
    }

    @Test
    void voucherOfAnotherCustomerIsRejected() {
        Customer owner = Customer.builder().id(5L).phone("0912345678").build();
        Voucher voucher = Voucher.builder().id(1L).code("NAM50K").customer(owner).discountValue(new BigDecimal("50000"))
                .status(VoucherStatus.ISSUED).build();
        when(voucherRepository.findByCodeIgnoreCase("NAM50K")).thenReturn(Optional.of(voucher));

        var quote = pricingService.quote(List.of(new PricingService.InputLine(latte, 2, null)),
                OrderType.TAKE_AWAY, "0987654321", "NAM50K", now);

        assertThat(quote.voucher()).isNull();
        assertThat(quote.voucherDiscount()).isEqualByComparingTo("0");
        assertThat(quote.voucherMessage()).contains("không thuộc về");
        assertThat(quote.total()).isEqualByComparingTo("100000");
    }

    @Test
    void soldOutCoffeeIsReported() {
        coldBrew.setStatus(CoffeeStatus.SOLD_OUT);
        var quote = pricingService.quote(List.of(new PricingService.InputLine(coldBrew, 1, null)),
                OrderType.TAKE_AWAY, null, null, now);
        assertThat(quote.unavailable()).containsExactly("Cold Brew");
    }

    private Promotion promotion(PromotionScope scope, String percent, String minOrder) {
        return Promotion.builder().id(scope == PromotionScope.ORDER ? 1L : 2L).name("KM").applyScope(scope)
                .discountPercent(percent == null ? null : new BigDecimal(percent))
                .minOrderAmount(minOrder == null ? null : new BigDecimal(minOrder))
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).status(PromotionStatus.ACTIVE).build();
    }
}
