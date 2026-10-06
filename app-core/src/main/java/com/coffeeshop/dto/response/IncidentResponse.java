package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Incident;
import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.IncidentSeverity;
import com.coffeeshop.entity.enums.IncidentStatus;
import com.coffeeshop.entity.enums.IncidentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(Long id, Long orderId, String orderCode, String handledByName,
                               IncidentType type, IncidentSeverity severity, String reason, String description,
                               String resolution, IncidentStatus status, LocalDateTime reportedAt, String reportedBy,
                               LocalDateTime resolvedAt, List<VoucherBrief> vouchers) {

    public record VoucherBrief(String code, BigDecimal discountValue, LocalDateTime expiresAt) {
    }

    public static IncidentResponse from(Incident i, List<Voucher> vouchers) {
        return new IncidentResponse(i.getId(), i.getOrder().getId(), i.getOrder().getOrderCode(),
                i.getHandledBy() == null ? null : i.getHandledBy().getFullName(),
                i.getType(), i.getSeverity(), i.getReason(), i.getDescription(), i.getResolution(), i.getStatus(),
                i.getReportedAt(), i.getCreatedBy(), i.getResolvedAt(),
                vouchers.stream().map(v -> new VoucherBrief(v.getCode(), v.getDiscountValue(), v.getExpiresAt()))
                        .toList());
    }
}
