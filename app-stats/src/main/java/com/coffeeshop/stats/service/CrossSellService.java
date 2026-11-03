
package com.coffeeshop.stats.service;

import com.coffeeshop.stats.entity.*;
import com.coffeeshop.stats.entity.repository.*;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CrossSellService {

    private final CrossSellRuleRepository rules;
    private final CrossSellItemRepository items;
    private final CrossSellEventRepository events;
    private final CoffeeRepository coffees;

    public record ItemInput(Long coffeeId, int position) {}

    public record RuleInput(
            Long anchorCoffeeId,
            int priority,
            BigDecimal minCartAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            List<ItemInput> items
    ) {}

    public record EventInput(
            Long ruleId,
            Long coffeeId,
            String impressionId,
            String eventType
    ) {}

    private ResponseStatusException bad(String text) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, text
        );
    }

    private CrossSellRule requireRule(long id) {
        return rules.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy quy tắc"
                )
        );
    }

    private Coffee requireCoffee(Long id) {
        if (id == null) {
            throw bad("Thiếu mã sản phẩm");
        }

        return coffees.findById(id).orElseThrow(() ->
                bad("Sản phẩm không tồn tại hoặc đã xóa: " + id)
        );
    }

    private String generateRuleName(Coffee coffee) {
        String coffeeName = coffee.getName();

        if (coffeeName == null || coffeeName.isBlank()) {
            coffeeName = "Sản phẩm #" + coffee.getId();
        }

        String name = "Bán kèm - " + coffeeName.trim();

        return name.length() > 150
                ? name.substring(0, 150)
                : name;
    }

    private void validate(RuleInput r) {
        if (r == null || r.anchorCoffeeId() == null) {
            throw bad("Chọn sản phẩm gốc");
        }

        if (r.priority() < 0 || r.priority() > 10000) {
            throw bad("Độ ưu tiên từ 0 đến 10000");
        }

        if (r.minCartAmount() != null
                && r.minCartAmount().signum() < 0) {
            throw bad("Giá trị giỏ tối thiểu không được âm");
        }

        if (r.startsAt() != null
                && r.endsAt() != null
                && !r.startsAt().isBefore(r.endsAt())) {
            throw bad("Thời gian bắt đầu phải trước kết thúc");
        }

        if (r.items() == null || r.items().isEmpty()) {
            throw bad("Chọn ít nhất một sản phẩm đề xuất");
        }

        requireCoffee(r.anchorCoffeeId());

        Set<Long> ids = new HashSet<>();

        for (ItemInput item : r.items()) {
            if (item == null || item.coffeeId() == null) {
                throw bad("Thiếu sản phẩm liên quan");
            }

            if (item.coffeeId().equals(r.anchorCoffeeId())) {
                throw bad("Sản phẩm liên quan phải khác sản phẩm gốc");
            }

            if (!ids.add(item.coffeeId())) {
                throw bad("Không được thêm trùng sản phẩm liên quan");
            }

            if (item.position() < 0) {
                throw bad("Thứ tự phải không âm");
            }

            requireCoffee(item.coffeeId());
        }
    }

    private void apply(CrossSellRule entity, RuleInput input) {
        Coffee anchor = requireCoffee(input.anchorCoffeeId());

        entity.setAnchorCoffee(anchor);
        entity.setName(generateRuleName(anchor));
        entity.setPriority(input.priority());

        entity.setMinCartAmount(
                input.minCartAmount() == null
                        ? BigDecimal.ZERO
                        : input.minCartAmount()
        );

        entity.setStartsAt(input.startsAt());
        entity.setEndsAt(input.endsAt());
        entity.setUpdatedBy("admin");
        entity.setUpdatedAt(LocalDateTime.now());

        for (ItemInput inputItem : input.items()) {
            CrossSellItem item = new CrossSellItem();

            item.setRule(entity);
            item.setCoffee(requireCoffee(inputItem.coffeeId()));
            item.setPosition(inputItem.position());

            entity.getItems().add(item);
        }
    }

    private Map<String, Object> itemMap(CrossSellItem item) {
        Coffee coffee = item.getCoffee();

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", item.getId());
        result.put("coffeeId", coffee.getId());
        result.put("coffeeName", coffee.getName());
        result.put("price", coffee.getPrice());
        result.put("position", item.getPosition());

        return result;
    }

    private Map<String, Object> ruleMap(CrossSellRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", rule.getId());
        map.put("name", rule.getName());

        map.put(
                "anchor_coffee_id",
                rule.getAnchorCoffee().getId()
        );

        map.put(
                "anchor_name",
                rule.getAnchorCoffee().getName()
        );

        map.put("priority", rule.getPriority());
        map.put("min_cart_amount", rule.getMinCartAmount());
        map.put("starts_at", rule.getStartsAt());
        map.put("ends_at", rule.getEndsAt());
        map.put("status", rule.getStatus());
        map.put("created_by", rule.getCreatedBy());
        map.put("created_at", rule.getCreatedAt());
        map.put("updated_by", rule.getUpdatedBy());
        map.put("updated_at", rule.getUpdatedAt());

        map.put(
                "impressions",
                events.countByRuleIdAndEventType(
                        rule.getId(), "IMPRESSION"
                )
        );

        map.put(
                "clicks",
                events.countByRuleIdAndEventType(
                        rule.getId(), "CLICK"
                )
        );

        map.put(
                "items",
                items.findByRuleIdOrderByPositionAscIdAsc(
                        rule.getId()
                ).stream().map(this::itemMap).toList()
        );

        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rules() {
        return rules.findAllByOrderByPriorityDescIdDesc()
                .stream()
                .map(this::ruleMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> one(long id) {
        return ruleMap(requireRule(id));
    }

    @Transactional
    public Map<String, Object> create(RuleInput input) {
        validate(input);

        CrossSellRule rule = new CrossSellRule();

        apply(rule, input);

        if (rule.getName() == null || rule.getName().isBlank()) {
            throw bad("Tên quy tắc không được để trống");
        }

        if (rule.getStatus() == null) {
            rule.setStatus("DRAFT");
        }

        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(LocalDateTime.now());
        }

        if (rule.getCreatedBy() == null) {
            rule.setCreatedBy("admin");
        }

        rules.saveAndFlush(rule);

        return ruleMap(rule);
    }

    @Transactional
    public Map<String, Object> update(long id, RuleInput input) {
        CrossSellRule rule = requireRule(id);

        validate(input);

        if ("ACTIVE".equals(rule.getStatus())) {
            throw bad("Tạm dừng quy tắc trước khi chỉnh sửa");
        }

        rule.getItems().clear();
        rules.flush();

        apply(rule, input);
        rules.saveAndFlush(rule);

        return ruleMap(rule);
    }

    @Transactional
    public Map<String, Object> status(long id, String next) {
        CrossSellRule rule = requireRule(id);

        if (!List.of("DRAFT", "ACTIVE", "PAUSED").contains(next)) {
            throw bad("Trạng thái quy tắc không hợp lệ");
        }

        if ("ACTIVE".equals(next) && rule.getItems().isEmpty()) {
            throw bad("Không thể kích hoạt quy tắc trống");
        }

        rule.setStatus(next);
        rule.setUpdatedBy("admin");
        rule.setUpdatedAt(LocalDateTime.now());

        rules.saveAndFlush(rule);

        return ruleMap(rule);
    }

    @Transactional
    public void delete(long id) {
        rules.delete(requireRule(id));
    }

    private Map<String, Object> suggestion(
            CrossSellRule rule,
            CrossSellItem item,
            boolean includeRule
    ) {
        Coffee coffee = item.getCoffee();

        Map<String, Object> map = new LinkedHashMap<>();

        if (includeRule) {
            map.put("ruleId", rule.getId());
        }

        map.put("coffeeId", coffee.getId());
        map.put("coffeeName", coffee.getName());
        map.put("price", coffee.getPrice());
        map.put("description", coffee.getDescription());
        map.put("imageUrl", coffee.getImageUrl());
        map.put("imageId", coffee.getImageId());
        map.put("position", item.getPosition());

        if (includeRule) {
            map.put("priority", rule.getPriority());
        }

        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recommend(
            long anchorId,
            BigDecimal total,
            int limit
    ) {
        int cap = Math.max(1, Math.min(limit, 12));

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (CrossSellRule rule : rules.findEligible(
                anchorId,
                total == null ? BigDecimal.ZERO : total,
                LocalDateTime.now()
        )) {
            for (CrossSellItem item :
                    items.findByRuleIdOrderByPositionAscIdAsc(
                            rule.getId()
                    )) {

                Coffee coffee = item.getCoffee();

                if (coffee.getStatus() != CoffeeStatus.AVAILABLE
                        || coffee.isAutoSoldOut()) {
                    continue;
                }

                if (!seen.add(coffee.getId())) {
                    continue;
                }

                result.add(suggestion(rule, item, true));

                if (result.size() >= cap) {
                    return result;
                }
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> preview(long id) {
        CrossSellRule rule = requireRule(id);

        return items.findByRuleIdOrderByPositionAscIdAsc(id)
                .stream()
                .map(item -> suggestion(rule, item, false))
                .toList();
    }

    // FIX: Ghi nhận impression/click không bị lỗi duplicate key
    @Transactional
    public void event(EventInput input) {
        if (input == null
                || input.ruleId() == null
                || input.coffeeId() == null) {
            throw bad("Thiếu dữ liệu tương tác");
        }

        if (!List.of("IMPRESSION", "CLICK")
                .contains(input.eventType())) {
            throw bad("Loại tương tác không hợp lệ");
        }

        UUID impression;

        try {
            impression = UUID.fromString(input.impressionId());
        } catch (Exception ex) {
            throw bad("Mã lần hiển thị không hợp lệ");
        }

        CrossSellRule rule = requireRule(input.ruleId());

        if (!"ACTIVE".equals(rule.getStatus())) {
            throw bad("Quy tắc chưa kích hoạt");
        }

        boolean allowed = items
                .findByRuleIdOrderByPositionAscIdAsc(rule.getId())
                .stream()
                .anyMatch(item ->
                        item.getCoffee().getId()
                                .equals(input.coffeeId())
                );

        if (!allowed) {
            throw bad("Món không thuộc quy tắc");
        }

        // PostgreSQL xử lý ghi trùng một cách atomic.
        // Cùng impressionId + coffeeId + eventType
        // chỉ được lưu một bản ghi.
        events.insertIfAbsent(
                rule.getId(),
                input.coffeeId(),
                impression,
                input.eventType()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics(long id) {
        requireRule(id);

        long views = events.countByRuleIdAndEventType(
                id, "IMPRESSION"
        );

        long clicks = events.countByRuleIdAndEventType(
                id, "CLICK"
        );

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("impressions", views);
        map.put("clicks", clicks);

        map.put(
                "clickThroughRate",
                views == 0 ? 0d : (double) clicks / views
        );

        map.put("purchasesTracked", false);

        map.put(
                "note",
                "Chưa có liên kết order attribution: "
                        + "không suy diễn lượt mua từ click"
        );

        return map;
    }
}
