package com.coffeeshop.dto.response;

import java.util.List;

public record InventoryAlertsResponse(List<MaterialResponse> lowStock, List<BatchResponse> expiringBatches) {
}
