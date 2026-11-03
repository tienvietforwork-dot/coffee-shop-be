package com.coffeeshop.service;

import com.coffeeshop.dto.response.InventoryReportResponse;
import com.coffeeshop.entity.InventorySnapshot;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.repository.InventorySnapshotRepository;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.MaterialTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo xuất – nhập – tồn, worked back from today's stock and the stock movements:
 * tồn cuối kỳ = tồn hiện tại − phát sinh sau kỳ; tồn đầu kỳ = tồn cuối kỳ − phát sinh trong kỳ.
 * A job closes the stock every evening (inventory_snapshots) for the day-by-day chart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialTransactionRepository transactionRepository;
    private final InventorySnapshotRepository snapshotRepository;

    /** in / out of one material over a window, by type: [qty in, qty out, value in, value out] (out negative) */
    private record Flow(EnumMap<MaterialTransactionType, BigDecimal[]> byType) {
        BigDecimal[] of(MaterialTransactionType t) {
            return byType.getOrDefault(t, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }

        BigDecimal netQuantity() {
            return byType.values().stream().map(v -> v[0].add(v[1])).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal netValue() {
            return byType.values().stream().map(v -> v[2].add(v[3])).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse report(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        Map<Long, Flow> inPeriod = flows(transactionRepository.movements(start, end));
        Map<Long, Flow> after = flows(transactionRepository.movements(end, null));
        Map<Long, BigDecimal[]> now = new HashMap<>();
        for (Object[] r : batchRepository.stockValues()) now.put((Long) r[0], new BigDecimal[]{(BigDecimal) r[1], (BigDecimal) r[2]});

        List<InventoryReportResponse.Row> rows = new ArrayList<>();
        BigDecimal openingTotal = BigDecimal.ZERO, inTotal = BigDecimal.ZERO, outTotal = BigDecimal.ZERO, closingTotal = BigDecimal.ZERO;
        Flow empty = new Flow(new EnumMap<>(MaterialTransactionType.class));
        for (Material m : materialRepository.findAllByOrderByNameAsc()) {
            Flow p = inPeriod.getOrDefault(m.getId(), empty);
            Flow a = after.getOrDefault(m.getId(), empty);
            BigDecimal[] current = now.getOrDefault(m.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal closingQty = m.getStockQuantity().subtract(a.netQuantity());
            BigDecimal closingValue = current[1].subtract(a.netValue());
            BigDecimal openingQty = closingQty.subtract(p.netQuantity());
            BigDecimal openingValue = closingValue.subtract(p.netValue());

            BigDecimal inPurchase = p.of(MaterialTransactionType.IMPORT)[0];
            BigDecimal inProduce = p.of(MaterialTransactionType.PRODUCE)[0];
            BigDecimal inAdjust = p.of(MaterialTransactionType.ADJUSTMENT)[0];
            BigDecimal inQty = p.byType().values().stream().map(v -> v[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal inValue = p.byType().values().stream().map(v -> v[2]).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outSale = p.of(MaterialTransactionType.SALE)[1].negate();
            BigDecimal outProduction = p.of(MaterialTransactionType.PRODUCTION_USE)[1].negate();
            BigDecimal outDisposal = p.of(MaterialTransactionType.DISPOSAL)[1].negate();
            BigDecimal outReturn = p.of(MaterialTransactionType.SUPPLIER_RETURN)[1].negate();
            BigDecimal outQty = p.byType().values().stream().map(v -> v[1]).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
            BigDecimal outValue = p.byType().values().stream().map(v -> v[3]).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
            BigDecimal outOther = outQty.subtract(outSale).subtract(outProduction).subtract(outDisposal).subtract(outReturn);

            if (openingQty.signum() == 0 && inQty.signum() == 0 && outQty.signum() == 0 && closingQty.signum() == 0) continue;
            rows.add(new InventoryReportResponse.Row(m.getId(), m.getName(), m.getUnit(), m.getKind().name(), m.getMinStock(),
                    openingQty, openingValue, inPurchase, inProduce, inAdjust, inQty, inValue,
                    outSale, outProduction, outDisposal, outReturn, outOther, outQty, outValue, closingQty, closingValue));
            openingTotal = openingTotal.add(openingValue);
            inTotal = inTotal.add(inValue);
            outTotal = outTotal.add(outValue);
            closingTotal = closingTotal.add(closingValue);
        }
        return new InventoryReportResponse(from, to, openingTotal, inTotal, outTotal, closingTotal, rows);
    }

    /** day-by-day closing stock from the snapshots */
    @Transactional(readOnly = true)
    public List<InventoryReportResponse.Daily> daily(LocalDate from, LocalDate to, Long materialId) {
        return snapshotRepository.findBetween(from, to, materialId).stream().map(s -> new InventoryReportResponse.Daily(
                s.getSnapshotDate(), s.getMaterial().getId(), s.getMaterial().getName(), s.getQuantity(), s.getValue())).toList();
    }

    /** Chốt tồn cuối ngày: one row per material (re-run on the same day replaces that day). */
    @Scheduled(cron = "${app.inventory.snapshot-cron:0 55 23 * * *}")
    @Transactional
    public int closeDay() {
        LocalDate today = LocalDate.now();
        snapshotRepository.deleteByDate(today);
        Map<Long, BigDecimal> values = new HashMap<>();
        for (Object[] r : batchRepository.stockValues()) values.put((Long) r[0], (BigDecimal) r[2]);
        LocalDateTime now = LocalDateTime.now();
        List<InventorySnapshot> rows = materialRepository.findAllByOrderByNameAsc().stream()
                .map(m -> InventorySnapshot.builder().snapshotDate(today).material(m).quantity(m.getStockQuantity())
                        .value(values.getOrDefault(m.getId(), BigDecimal.ZERO)).createdAt(now).build())
                .toList();
        snapshotRepository.saveAll(rows);
        log.info("Chốt tồn kho ngày {}: {} nguyên liệu", today, rows.size());
        return rows.size();
    }

    private static Map<Long, Flow> flows(List<Object[]> raw) {
        Map<Long, Flow> result = new HashMap<>();
        for (Object[] r : raw) {
            Flow f = result.computeIfAbsent((Long) r[0], k -> new Flow(new EnumMap<>(MaterialTransactionType.class)));
            f.byType().put((MaterialTransactionType) r[1], new BigDecimal[]{
                    nz(r[2]), nz(r[3]), nz(r[4]), nz(r[5])});
        }
        return result;
    }

    private static BigDecimal nz(Object v) {
        return v == null ? BigDecimal.ZERO : v instanceof BigDecimal d ? d : new BigDecimal(v.toString());
    }
}
