package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Stocktake;
import com.coffeeshop.entity.StocktakeItem;
import com.coffeeshop.entity.enums.StockQuality;
import com.coffeeshop.entity.enums.StocktakeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Phiếu kiểm kê with its lines (biên bản kiểm kê vật tư, mẫu 05-VT). */
public record StocktakeResponse(Long id, String code, StocktakeStatus status, String staffName, String members,
                                LocalDateTime countedAt, String balancedByName, LocalDateTime balancedAt,
                                String resolution, String note, int lineCount, int countedCount,
                                BigDecimal surplusValue, BigDecimal shortageValue, LocalDateTime createdAt,
                                String createdBy, List<Line> items) {

    public record Line(Long id, Long batchId, Long materialId, String materialName, String unit, LocalDate expiryDate,
                       BigDecimal bookQuantity, BigDecimal countedQuantity, BigDecimal difference, BigDecimal unitCost,
                       BigDecimal differenceValue, StockQuality quality, String reason) {
        static Line from(StocktakeItem i) {
            var b = i.getBatch();
            return new Line(i.getId(), b.getId(), b.getMaterial().getId(), b.getMaterial().getName(), b.getMaterial().getUnit(),
                    b.getExpiryDate(), i.getBookQuantity(), i.getCountedQuantity(), i.difference(), i.getUnitCost(),
                    i.differenceValue(), i.getQuality(), i.getReason());
        }
    }

    public static StocktakeResponse from(Stocktake s, boolean withItems) {
        return new StocktakeResponse(s.getId(), s.getCode(), s.getStatus(),
                s.getStaff() == null ? null : s.getStaff().getFullName(), s.getMembers(), s.getCountedAt(),
                s.getBalancedBy() == null ? null : s.getBalancedBy().getFullName(), s.getBalancedAt(), s.getResolution(),
                s.getNote(), s.getItems().size(), (int) s.getItems().stream().filter(i -> i.getCountedQuantity() != null).count(),
                s.surplusValue(), s.shortageValue(), s.getCreatedAt(), s.getCreatedBy(),
                withItems ? s.getItems().stream().map(Line::from).toList() : null);
    }
}
