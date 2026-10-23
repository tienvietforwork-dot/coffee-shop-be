package com.coffeeshop.crm.dto.response;

import java.time.LocalDateTime;

public record CustomerPortalInteractionReadReceiptResponse(Long interactionId, LocalDateTime readAt) { }
