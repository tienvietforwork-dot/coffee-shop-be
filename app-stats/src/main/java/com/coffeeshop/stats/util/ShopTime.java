package com.coffeeshop.stats.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Business-facing local schedules are configured in Vietnam time, independent of host TZ. */
public final class ShopTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private ShopTime() { }
    public static LocalDateTime now() { return LocalDateTime.now(ZONE); }
    public static LocalDate today() { return LocalDate.now(ZONE); }
}
