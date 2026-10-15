package com.coffeeshop.crm.dto.response;

import java.time.LocalDateTime;

public record CustomerPortalReadReceiptResponse(Long feedbackId, Long responseId, LocalDateTime readAt) { }
