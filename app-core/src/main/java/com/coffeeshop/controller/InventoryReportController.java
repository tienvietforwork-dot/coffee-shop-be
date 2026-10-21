package com.coffeeshop.controller;

import com.coffeeshop.dto.response.CoffeeCostResponse;
import com.coffeeshop.dto.response.InventoryReportResponse;
import com.coffeeshop.export.ExcelSheet;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CoffeeCostService;
import com.coffeeshop.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Báo cáo xuất – nhập – tồn (+ Excel, chốt tồn theo ngày) and giá vốn món. */
@RestController
@RequiredArgsConstructor
public class InventoryReportController {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InventoryReportService reportService;
    private final CoffeeCostService costService;

    @GetMapping("/api/reports/inventory")
    @PreAuthorize(Perm.CAN_INVENTORY_REPORT)
    public InventoryReportResponse inventory(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.report(from, to);
    }

    @GetMapping("/api/reports/inventory.xlsx")
    @PreAuthorize(Perm.CAN_INVENTORY_REPORT)
    public ResponseEntity<byte[]> inventoryExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        InventoryReportResponse r = reportService.report(from, to);
        ExcelSheet x = new ExcelSheet("Xuat nhap ton")
                .title("BÁO CÁO TỔNG HỢP XUẤT – NHẬP – TỒN NGUYÊN LIỆU")
                .line("Từ ngày " + D.format(from) + " đến ngày " + D.format(to))
                .header("Nguyên liệu", "ĐVT", "Tồn đầu (SL)", "Tồn đầu (giá trị)", "Nhập mua", "Nhập chế biến", "Kiểm kê thừa",
                        "Tổng nhập (SL)", "Tổng nhập (giá trị)", "Xuất bán", "Xuất chế biến", "Hủy", "Trả NCC", "Xuất khác",
                        "Tổng xuất (SL)", "Tổng xuất (giá trị)", "Tồn cuối (SL)", "Tồn cuối (giá trị)");
        for (var row : r.rows()) {
            x.row(row.materialName(), row.unit(), row.openingQuantity(), row.openingValue(), row.inPurchase(), row.inProduce(),
                    row.inAdjust(), row.inQuantity(), row.inValue(), row.outSale(), row.outProduction(), row.outDisposal(),
                    row.outReturn(), row.outOther(), row.outQuantity(), row.outValue(), row.closingQuantity(), row.closingValue());
        }
        x.total("Cộng", "", null, r.openingValue(), null, null, null, null, r.inValue(), null, null, null, null, null, null,
                r.outValue(), null, r.closingValue()).mergeTitle();
        return x.download("xuat-nhap-ton-" + from + "_" + to);
    }

    @GetMapping("/api/reports/inventory/daily")
    @PreAuthorize(Perm.CAN_INVENTORY_REPORT)
    public List<InventoryReportResponse.Daily> daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) Long materialId) {
        return reportService.daily(from, to, materialId);
    }

    /** Chốt tồn ngay (normally the evening job does it). */
    @PostMapping("/api/reports/inventory/close-day")
    @PreAuthorize(Perm.CAN_INVENTORY_REPORT)
    public Map<String, Integer> closeDay() {
        return Map.of("materials", reportService.closeDay());
    }

    @GetMapping("/api/coffee-costs")
    @PreAuthorize(Perm.CAN_COFFEE_COSTS)
    public List<CoffeeCostResponse> costs() {
        return costService.findAll();
    }

    @GetMapping("/api/coffee-costs/{coffeeId}/history")
    @PreAuthorize(Perm.CAN_COFFEE_COSTS)
    public List<CoffeeCostResponse.History> costHistory(@PathVariable Long coffeeId) {
        return costService.history(coffeeId);
    }

    @PostMapping("/api/coffee-costs/recalculate")
    @PreAuthorize(Perm.CAN_COFFEE_COSTS)
    public Map<String, Integer> recalculate() {
        return Map.of("changed", costService.recalculate());
    }

    /** body: { "price": 39000 } */
    @PutMapping("/api/coffee-costs/{coffeeId}/price")
    @PreAuthorize(Perm.CAN_COFFEE_COSTS_EDIT)
    public CoffeeCostResponse updatePrice(@PathVariable Long coffeeId, @RequestBody Map<String, BigDecimal> body) {
        return costService.updatePrice(coffeeId, body.get("price"));
    }
}
