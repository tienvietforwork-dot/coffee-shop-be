package com.coffeeshop.service;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Recipe;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.CoffeeRepository;
import com.coffeeshop.repository.RecipeRepository;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * How many cups each coffee can still be made from current stock:
 * cups = min over the recipe's materials of floor(stock / quantity per cup).
 * Also keeps coffee status in step with it: at 0 cups AVAILABLE becomes SOLD_OUT, and a SOLD_OUT
 * that stock caused reopens once the stock is back (a SOLD_OUT set by hand is left alone).
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final RecipeRepository recipeRepository;
    private final CoffeeRepository coffeeRepository;
    private final NotificationService notificationService;

    /** One material of a coffee's active recipe, with what the stock allows. */
    public record StockLine(Long materialId, String materialName, String unit, BigDecimal stock, BigDecimal perCup, int cups) {
    }

    /** Active recipes' per-cup needs; a coffee without an active recipe is absent (not limited by stock). */
    public record Snapshot(Map<Long, List<StockLine>> byCoffee) {

        public boolean hasRecipe(Long coffeeId) {
            return byCoffee.containsKey(coffeeId);
        }

        public List<StockLine> lines(Long coffeeId) {
            return byCoffee.getOrDefault(coffeeId, List.of());
        }

        /** null = unlimited (no recipe or a recipe without materials). */
        public Integer servings(Long coffeeId) {
            return lines(coffeeId).stream().map(StockLine::cups).min(Integer::compare).orElse(null);
        }

        /** Current stock of every material used by an active recipe. */
        public Map<Long, BigDecimal> stock() {
            Map<Long, BigDecimal> stock = new TreeMap<>();
            byCoffee.values().forEach(ls -> ls.forEach(l -> stock.put(l.materialId(), l.stock())));
            return stock;
        }

        public Map<Long, BigDecimal> perCup(Long coffeeId) {
            return lines(coffeeId).stream().collect(Collectors.toMap(StockLine::materialId, StockLine::perCup, (a, b) -> a, TreeMap::new));
        }
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Map<Long, List<StockLine>> byCoffee = new HashMap<>();
        for (Recipe r : recipeRepository.findAllActiveWithMaterials()) {
            byCoffee.put(r.getCoffee().getId(), r.getMaterials().stream().map(rm -> new StockLine(
                    rm.getMaterial().getId(), rm.getMaterial().getName(), rm.getMaterial().getUnit(),
                    rm.getMaterial().getStockQuantity(), rm.getQuantity(),
                    rm.getMaterial().getStockQuantity().divide(rm.getQuantity(), 0, RoundingMode.FLOOR).intValue())).toList());
        }
        return new Snapshot(byCoffee);
    }


    public record StatusChange(Long coffeeId, String name, CoffeeStatus status) {
    }

    /** Call after anything that changes stock or recipes; flips AVAILABLE ⇄ SOLD_OUT and tells admin screens. */
    @Transactional
    public List<StatusChange> syncCoffeeStatus() {
        Snapshot snap = snapshot();
        List<StatusChange> changes = new ArrayList<>();
        for (Coffee c : coffeeRepository.findByStatusInOrderByNameAsc(List.of(CoffeeStatus.AVAILABLE, CoffeeStatus.SOLD_OUT))) {
            boolean out = Integer.valueOf(0).equals(snap.servings(c.getId()));
            if (out && c.getStatus() == CoffeeStatus.AVAILABLE) {
                c.setStatus(CoffeeStatus.SOLD_OUT);
                c.setAutoSoldOut(true);
            } else if (!out && c.getStatus() == CoffeeStatus.SOLD_OUT && c.isAutoSoldOut()) {
                c.setStatus(CoffeeStatus.AVAILABLE);
                c.setAutoSoldOut(false);
            } else {
                continue;
            }
            changes.add(new StatusChange(c.getId(), c.getName(), c.getStatus()));
        }
        if (!changes.isEmpty()) afterCommit(() -> notificationService.notifyMenuChanged(changes));
        return changes;
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
    /** Rejects an order whose lines together need more of some material than is in stock. */
    @Transactional(readOnly = true)
    public void requireStock(List<PricingService.InputLine> lines) {
        Snapshot snap = snapshot();
        Map<Long, BigDecimal> stock = snap.stock();
        Map<Long, BigDecimal> needed = new HashMap<>();
        for (var line : lines) {
            snap.perCup(line.coffee().getId()).forEach((materialId, perCup) ->
                    needed.merge(materialId, perCup.multiply(BigDecimal.valueOf(line.quantity())), BigDecimal::add));
        }
        Set<Long> lacking = needed.entrySet().stream()
                .filter(e -> e.getValue().compareTo(stock.get(e.getKey())) > 0)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        if (lacking.isEmpty()) return;

        Map<Long, StockLine> info = new HashMap<>();
        snap.byCoffee().values().forEach(ls -> ls.forEach(l -> info.put(l.materialId(), l)));
        String detail = lacking.stream().map(info::get).map(l -> l.materialName() + " (cần " + plain(needed.get(l.materialId()))
                + " " + l.unit() + ", còn " + plain(l.stock()) + " " + l.unit() + ")").collect(Collectors.joining("; "));
        throw new BadRequestException("Không đủ nguyên liệu cho đơn này: " + detail);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
