package com.coffeeshop.stats.entity;

import com.coffeeshop.stats.entity.enums.ProductBundleStatus;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MerchandisingAvailabilityTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 8, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 27, 10, 0);

    @Test void bundleIsActiveAtStartButNotAtEnd() {
        ProductBundle bundle = new ProductBundle();
        bundle.setStatus(ProductBundleStatus.ACTIVE);
        bundle.setStartsAt(START);
        bundle.setEndsAt(END);
        assertFalse(bundle.isAvailableNow(START.minusNanos(1)));
        assertTrue(bundle.isAvailableNow(START));
        assertTrue(bundle.isAvailableNow(END.minusNanos(1)));
        assertFalse(bundle.isAvailableNow(END));
    }

    @Test void placementIsVisibleOnlyInsidePublishedWindow() {
        ProductPlacementRule rule = new ProductPlacementRule();
        rule.setPublicationStatus(PublicationStatus.PUBLISHED);
        rule.setActive(true);
        rule.setStartsAt(START);
        rule.setEndsAt(END);
        assertFalse(rule.isVisibleNow(START.minusSeconds(1)));
        assertTrue(rule.isVisibleNow(START));
        assertFalse(rule.isVisibleNow(END));
        rule.setPublicationStatus(PublicationStatus.DRAFT);
        assertFalse(rule.isVisibleNow(START.plusSeconds(1)));
    }
}
