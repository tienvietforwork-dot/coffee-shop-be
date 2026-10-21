package com.coffeeshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo tổng hợp xuất – nhập – tồn for a period. Quantities in each material's unit, values at lot cost.
 * Nhập = mua (IMPORT) + chế biến (PRODUCE) + kiểm kê thừa; xuất = bán + xuất chế biến + hủy + trả NCC + khác
 * (xuất dùng nội bộ, hủy phiếu nhập, kiểm kê thiếu).
 */
public record InventoryReportResponse(LocalDate from, LocalDate to, BigDecimal openingValue, BigDecimal inValue,
                                      BigDecimal outValue, BigDecimal closingValue, List<Row> rows) {

    public record Row(Long materialId, String materialName, String unit, String kind, BigDecimal minStock,
                      BigDecimal openingQuantity, BigDecimal openingValue,
                      BigDecimal inPurchase, BigDecimal inProduce, BigDecimal inAdjust, BigDecimal inQuantity, BigDecimal inValue,
                      BigDecimal outSale, BigDecimal outProduction, BigDecimal outDisposal, BigDecimal outReturn,
                      BigDecimal outOther, BigDecimal outQuantity, BigDecimal outValue,
                      BigDecimal closingQuantity, BigDecimal closingValue) {
    }

    /** closing stock per day (from the end-of-day snapshots) */
    public record Daily(LocalDate date, Long materialId, String materialName, BigDecimal quantity, BigDecimal value) {
    }
}
