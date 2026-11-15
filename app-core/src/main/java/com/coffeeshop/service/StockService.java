package com.coffeeshop.service;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderItem;
import com.coffeeshop.entity.Recipe;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Số lượng nguyên liệu theo đơn:
 * <ul>
 *   <li>đơn bắt đầu pha (xác nhận / đơn tại quầy) → trừ nguyên liệu theo công thức đang dùng × số ly;</li>
 *   <li>đơn đã trừ mà bị hủy / từ chối → cộng lại;</li>
 *   <li>sau mỗi thay đổi số lượng hoặc công thức: món không đủ nguyên liệu cho 1 ly tự "Hết món",
 *       đủ lại thì tự mở bán (chỉ các món do hệ thống tự đánh hết — {@link Coffee#isAutoSoldOut()}).</li>
 * </ul>
 * Món chưa có công thức không bị theo dõi.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final RecipeRepository recipeRepository;

    /** Trừ nguyên liệu cho đơn; thiếu thì báo lỗi (cả giao dịch không được lưu). */
    @Transactional
    public void deduct(Order order) {
        if (order.isStockDeducted()) return;
        Map<Material, BigDecimal> need = need(order.getItems());
        List<String> missing = new ArrayList<>();
        need.forEach((m, q) -> {
            if (m.getQuantity().compareTo(q) < 0) {
                missing.add("%s (cần %s %s, còn %s)".formatted(m.getName(), plain(q), m.getUnit(), plain(m.getQuantity())));
            }
        });
        if (!missing.isEmpty()) throw new BadRequestException("Không đủ nguyên liệu: " + String.join("; ", missing));
        need.forEach((m, q) -> m.setQuantity(m.getQuantity().subtract(q)));
        order.setStockDeducted(true);
        refreshAvailability();
    }

    /** Cộng lại nguyên liệu đã trừ (đơn hủy / từ chối sau khi đã bắt đầu pha). */
    @Transactional
    public void restore(Order order) {
        if (!order.isStockDeducted()) return;
        need(order.getItems()).forEach((m, q) -> m.setQuantity(m.getQuantity().add(q)));
        order.setStockDeducted(false);
        refreshAvailability();
    }

    /** Tự "Hết món" / mở bán lại theo số lượng nguyên liệu hiện có. */
    @Transactional
    public void refreshAvailability() {
        for (Recipe recipe : recipeRepository.findAllActiveWithMaterials()) {
            Coffee coffee = recipe.getCoffee();
            boolean enough = recipe.getMaterials().stream()
                    .allMatch(rm -> rm.getMaterial().getQuantity().compareTo(rm.getQuantity()) >= 0);
            if (!enough && coffee.getStatus() == CoffeeStatus.AVAILABLE) {
                coffee.setStatus(CoffeeStatus.SOLD_OUT);
                coffee.setAutoSoldOut(true);
            } else if (enough && coffee.getStatus() == CoffeeStatus.SOLD_OUT && coffee.isAutoSoldOut()) {
                coffee.setStatus(CoffeeStatus.AVAILABLE);
                coffee.setAutoSoldOut(false);
            }
        }
    }

    /** Tổng nguyên liệu cần cho các dòng đơn, theo công thức đang dùng. */
    private Map<Material, BigDecimal> need(List<OrderItem> items) {
        Map<Long, Recipe> recipes = recipeRepository.findAllActiveWithMaterials().stream()
                .collect(Collectors.toMap(r -> r.getCoffee().getId(), Function.identity(), (a, b) -> a));
        Map<Long, Material> byId = new HashMap<>();
        Map<Long, BigDecimal> total = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Recipe recipe = recipes.get(item.getCoffee().getId());
            if (recipe == null) continue;
            for (var rm : recipe.getMaterials()) {
                byId.put(rm.getMaterial().getId(), rm.getMaterial());
                total.merge(rm.getMaterial().getId(), rm.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
            }
        }
        Map<Material, BigDecimal> out = new LinkedHashMap<>();
        total.forEach((id, q) -> out.put(byId.get(id), q));
        return out;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
