package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** "Tài khoản của tôi" for a logged-in customer. */
public record AccountResponse(Long customerId, String fullName, String phone, String email, Integer loyaltyPoints,
                              LocalDateTime registeredAt, long orderCount, BigDecimal totalSpent) {
}
