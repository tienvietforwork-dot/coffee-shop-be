package com.coffeeshop.service;

import com.coffeeshop.dto.response.CoffeeCostResponse;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.CoffeeCostSnapshot;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.MaterialComponent;
import com.coffeeshop.entity.Recipe;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CoffeeCostSnapshotRepository;
import com.coffeeshop.repository.CoffeeRepository;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Giá vốn món theo định lượng (food cost). A material costs the weighted average of its lots in stock; when none is
 * left, its latest purchase price; a prepared material costs its raw formula divided by what one batch yields.
 * The job re-costs every coffee each hour and keeps a row whenever the cost or the price changed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoffeeCostService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CoffeeRepository coffeeRepository;
    private final RecipeRepository recipeRepository;
    private final MaterialBatchRepository batchRepository;
    private final CoffeeCostSnapshotRepository snapshotRepository;

    /** cost per unit of each material, and where it came from */
    record Cost(BigDecimal value, String source) {
    }

    @Transactional(readOnly = true)
    public List<CoffeeCostResponse> findAll() {
        Map<Long, Cost> costs = materialCosts();
        Map<Long, Recipe> recipes = new HashMap<>();
        for (Recipe r : recipeRepository.findAllActiveWithMaterials()) recipes.put(r.getCoffee().getId(), r);
        return coffeeRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(c -> cost(c, recipes.get(c.getId()), costs)).toList();
    }

    @Transactional(readOnly = true)
    public List<CoffeeCostResponse.History> history(Long coffeeId) {
        return snapshotRepository.findByCoffeeIdOrderByComputedAtAsc(coffeeId).stream()
                .map(s -> new CoffeeCostResponse.History(s.getComputedAt(), s.getUnitCost(), s.getPrice())).toList();
    }

    /** Đổi giá bán (e.g. to reach the target food cost %). */
    @Transactional
    public CoffeeCostResponse updatePrice(Long coffeeId, BigDecimal price) {
        Coffee coffee = coffeeRepository.findById(coffeeId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món: " + coffeeId));
        if (price == null || price.signum() <= 0) throw new BadRequestException("Giá bán phải lớn hơn 0");
        coffee.setPrice(price.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY));
        CoffeeCostResponse result = cost(coffee, recipeRepository.findByCoffeeIdAndActiveTrue(coffeeId).orElse(null), materialCosts());
        record(coffee, result);
        return result;
    }

    /** Tính lại giá vốn: every hour, and on demand. Returns how many coffees changed. */
    @Scheduled(cron = "${app.costing.cron:0 5 * * * *}")
    @Transactional
    public int recalculate() {
        Map<Long, Cost> costs = materialCosts();
        Map<Long, Recipe> recipes = new HashMap<>();
        for (Recipe r : recipeRepository.findAllActiveWithMaterials()) recipes.put(r.getCoffee().getId(), r);
        int changed = 0;
        for (Coffee c : coffeeRepository.findAll()) {
            CoffeeCostResponse r = cost(c, recipes.get(c.getId()), costs);
            if (record(c, r)) changed++;
        }
        if (changed > 0) log.info("Tính lại giá vốn: {} món thay đổi", changed);
        return changed;
    }

    // ---- computation -----------------------------------------------------

    private boolean record(Coffee c, CoffeeCostResponse r) {
        if (r.recipeId() == null || !r.complete()) return false;
        var last = snapshotRepository.findFirstByCoffeeIdOrderByComputedAtDescIdDesc(c.getId()).orElse(null);
        if (last != null && last.getUnitCost().compareTo(r.unitCost()) == 0 && last.getPrice().compareTo(c.getPrice()) == 0) return false;
        snapshotRepository.save(CoffeeCostSnapshot.builder().coffee(c).recipeId(r.recipeId()).unitCost(r.unitCost())
                .price(c.getPrice()).computedAt(LocalDateTime.now()).build());
        return true;
    }

    private CoffeeCostResponse cost(Coffee c, Recipe recipe, Map<Long, Cost> costs) {
        var last = snapshotRepository.findFirstByCoffeeIdOrderByComputedAtDescIdDesc(c.getId()).orElse(null);
        String category = c.getCategory() == null ? null : c.getCategory().getName();
        if (recipe == null) {
            return new CoffeeCostResponse(c.getId(), c.getName(), category, c.getStatus().name(), null, c.getPrice(),
                    null, null, null, false, last == null ? null : last.getUnitCost(), last == null ? null : last.getComputedAt(), List.of());
        }
        List<CoffeeCostResponse.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean complete = true;
        for (var rm : recipe.getMaterials()) {
            Material m = rm.getMaterial();
            Cost unit = costs.get(m.getId());
            BigDecimal lineCost = unit == null ? null : vnd2(unit.value().multiply(rm.getQuantity()));
            if (lineCost == null) complete = false;
            else total = total.add(lineCost);
            lines.add(new CoffeeCostResponse.Line(m.getId(), m.getName(), m.getUnit(), rm.getQuantity(),
                    unit == null ? null : unit.value(), lineCost, unit == null ? "Chưa có giá nhập" : unit.source()));
        }
        BigDecimal unitCost = total.setScale(0, RoundingMode.HALF_UP);
        BigDecimal margin = c.getPrice().subtract(unitCost);
        BigDecimal percent = c.getPrice().signum() == 0 ? null
                : unitCost.multiply(HUNDRED).divide(c.getPrice(), 1, RoundingMode.HALF_UP);
        BigDecimal previous = null;
        LocalDateTime previousAt = null;
        if (last != null && last.getUnitCost().compareTo(unitCost) != 0) {
            previous = last.getUnitCost();
            previousAt = last.getComputedAt();
        }
        return new CoffeeCostResponse(c.getId(), c.getName(), category, c.getStatus().name(), recipe.getId(), c.getPrice(),
                unitCost, margin, percent, complete, previous, previousAt, lines);
    }

    /** cost per unit of every material */
    Map<Long, Cost> materialCosts() {
        Map<Long, BigDecimal[]> inStock = new HashMap<>(); // materialId → [qty with cost, value]
        Map<Long, BigDecimal> latest = new HashMap<>();
        Map<Long, Material> materials = new HashMap<>();
        for (MaterialBatch b : batchRepository.findCostedNewestFirst()) {
            Material m = b.getMaterial();
            materials.put(m.getId(), m);
            latest.putIfAbsent(m.getId(), b.getUnitCost());
            if (b.getStatus() == BatchStatus.AVAILABLE && b.getRemainingQuantity().signum() > 0) {
                BigDecimal[] acc = inStock.computeIfAbsent(m.getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                acc[0] = acc[0].add(b.getRemainingQuantity());
                acc[1] = acc[1].add(b.getRemainingQuantity().multiply(b.getUnitCost()));
            }
        }
        Map<Long, Cost> result = new HashMap<>();
        latest.forEach((id, price) -> {
            BigDecimal[] acc = inStock.get(id);
            result.put(id, acc != null && acc[0].signum() > 0
                    ? new Cost(acc[1].divide(acc[0], 4, RoundingMode.HALF_UP), "Bình quân lô tồn")
                    : new Cost(price, "Giá nhập gần nhất"));
        });
        // prepared materials: their raw formula ÷ yield (formulas only use raw materials, so one pass is enough)
        for (Recipe r : recipeRepository.findAllActiveWithMaterials()) {
            for (var rm : r.getMaterials()) {
                Material m = rm.getMaterial();
                if (m.getKind() != MaterialKind.PREPARED) continue;
                Hibernate.initialize(m.getComponents());
                if (m.getYieldQuantity() == null || m.getYieldQuantity().signum() == 0 || m.getComponents().isEmpty()) continue;
                BigDecimal sum = BigDecimal.ZERO;
                boolean known = true;
                for (MaterialComponent mc : m.getComponents()) {
                    Cost raw = result.get(mc.getComponent().getId());
                    if (raw == null) {
                        known = false;
                        break;
                    }
                    sum = sum.add(raw.value().multiply(mc.getQuantity()));
                }
                if (known) result.put(m.getId(), new Cost(sum.divide(m.getYieldQuantity(), 4, RoundingMode.HALF_UP), "Định mức chế biến"));
                else result.remove(m.getId());
            }
        }
        return result;
    }

    private static BigDecimal vnd2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
