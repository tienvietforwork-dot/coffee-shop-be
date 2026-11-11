
package com.coffeeshop.stats.service;

import com.coffeeshop.stats.entity.*;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UpsellService {

    private final UpsellRuleRepository rules;
    private final UpsellEventRepository events;
    private final CoffeeRepository coffees;
    private final JdbcTemplate jdbc;

    /** The manager curates the taste match between two different sellable coffees. */
    private boolean isVerifiedVariant(Long baseId, Long variantId, String type) {
        if (baseId == null || variantId == null || type == null
                || baseId.equals(variantId) || !"PREMIUM_VERSION".equals(type)) return false;
        Coffee base = coffees.findById(baseId).orElse(null);
        Coffee variant = coffees.findById(variantId).orElse(null);
        if (base == null || variant == null
                || variant.getPrice().compareTo(base.getPrice()) <= 0) return false;
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM public.upsell_variant_links
                WHERE base_coffee_id = ? AND variant_coffee_id = ?
                  AND upgrade_type = ? AND enabled = true
                  AND nullif(trim(experience_note), '') IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM public.recipes r
                      WHERE r.coffee_id = public.upsell_variant_links.variant_coffee_id
                        AND r.is_active = true AND r.del_flag = false
                  )
                """, Integer.class, baseId, variantId, type);
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> compatible(Long anchorId, String type) {
        if (anchorId == null || !"PREMIUM_VERSION".equals(type)) {
            return List.of();
        }
        List<Map<String, Object>> candidates = jdbc.queryForList("""
                SELECT c.id AS "coffeeId", c.name AS "coffeeName", c.price,
                       l.experience_note AS "experienceNote"
                FROM public.upsell_variant_links l
                JOIN public.coffees c ON c.id = l.variant_coffee_id
                JOIN public.coffees base ON base.id = l.base_coffee_id
                WHERE l.base_coffee_id = ? AND l.upgrade_type = ? AND l.enabled = true
                  AND nullif(trim(l.experience_note), '') IS NOT NULL
                  AND c.del_flag = false AND base.del_flag = false
                  AND c.status = 'AVAILABLE' AND c.auto_sold_out = false
                  AND c.id <> base.id
                  AND c.price > base.price
                ORDER BY c.price ASC, c.id ASC
                """, anchorId, type);
        return candidates;
    }

    /** Admin-curated relationship between two distinct menu items. */
    public record VariantInput(Long baseCoffeeId, Long variantCoffeeId,
                               String upgradeType, boolean recipeConfirmed,
                               boolean experienceConfirmed, String experienceNote) {}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> variants() {
        return jdbc.queryForList("""
                SELECT l.id, l.base_coffee_id AS "baseCoffeeId", b.name AS "baseName",
                       l.variant_coffee_id AS "variantCoffeeId", v.name AS "variantName",
                       l.upgrade_type AS "upgradeType", l.enabled,
                       l.experience_note AS "experienceNote",
                       b.price AS "basePrice", v.price AS "variantPrice"
                FROM public.upsell_variant_links l
                JOIN public.coffees b ON b.id=l.base_coffee_id
                JOIN public.coffees v ON v.id=l.variant_coffee_id
                ORDER BY l.id DESC
                """);
    }

    @Transactional
    public void registerVariant(VariantInput x) {
        if (x == null || x.baseCoffeeId() == null || x.variantCoffeeId() == null
                || !x.recipeConfirmed() || !x.experienceConfirmed()) {
            throw bad("Cần chọn 2 món, xác nhận công thức và xác nhận hai món cùng gu trải nghiệm");
        }
        if (x.baseCoffeeId().equals(x.variantCoffeeId())) throw bad("Phiên bản phải khác SKU gốc");
        if (!"PREMIUM_VERSION".equals(x.upgradeType())) {
            throw bad("Upsell sản phẩm chỉ hỗ trợ phiên bản cà phê cao cấp hơn");
        }
        String experienceNote = x.experienceNote() == null ? "" : x.experienceNote().trim();
        if (experienceNote.length() < 12 || experienceNote.length() > 500) {
            throw bad("Mô tả điểm chung về trải nghiệm cần từ 12 đến 500 ký tự");
        }
        Coffee base = coffee(x.baseCoffeeId());
        Coffee variant = coffee(x.variantCoffeeId());
        if (variant.getPrice().compareTo(base.getPrice()) <= 0) {
            throw bad("Món nâng cấp phải là món khác và có giá niêm yết cao hơn món gốc");
        }
        if (variant.getStatus() != CoffeeStatus.AVAILABLE || variant.isAutoSoldOut()) {
            throw bad("SKU nâng cấp chưa sẵn sàng để bán");
        }
        Integer activeRecipeCount = jdbc.queryForObject("""
                SELECT count(*) FROM public.recipes
                WHERE coffee_id = ? AND is_active = true AND del_flag = false
                """, Integer.class, variant.getId());
        if (activeRecipeCount == null || activeRecipeCount == 0) {
            throw bad("Món nâng cấp phải có công thức đang áp dụng trong app-core");
        }
        jdbc.update("""
                INSERT INTO public.upsell_variant_links
                (base_coffee_id, variant_coffee_id, upgrade_type, enabled, experience_note)
                VALUES (?, ?, ?, true, ?)
                ON CONFLICT (base_coffee_id, variant_coffee_id, upgrade_type)
                DO UPDATE SET enabled=true, experience_note=EXCLUDED.experience_note
                """, x.baseCoffeeId(), x.variantCoffeeId(), x.upgradeType(), experienceNote);
    }

    @Transactional
    public void deactivateVariant(long linkId) {
        int changed = jdbc.update("UPDATE public.upsell_variant_links SET enabled=false WHERE id=?", linkId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy liên kết nâng cấp");
    }

    public record RuleInput(
            String name,
            Long anchorCoffeeId,
            Long upgradeCoffeeId,
            String upgradeType,
            int priority,
            BigDecimal minCartAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {}

    public record EventInput(
            Long ruleId,
            String impressionId,
            String eventType
    ) {}

    private ResponseStatusException bad(String detail) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                detail
        );
    }

    private Coffee coffee(Long id) {
        if (id == null) {
            throw bad("Thiếu mã sản phẩm");
        }

        return coffees.findById(id)
                .orElseThrow(() ->
                        bad("Sản phẩm không tồn tại: " + id)
                );
    }

    private UpsellRule rule(long id) {
        return rules.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy quy tắc"
                        )
                );
    }

    // Áp dụng thông tin tạo hoặc chỉnh sửa quy tắc
    private void apply(UpsellRule r, RuleInput x) {

        if (x == null
                || x.anchorCoffeeId() == null
                || x.upgradeCoffeeId() == null) {
            throw bad(
                    "Phải chọn món gốc và phiên bản nâng cấp"
            );
        }

        if (x.anchorCoffeeId().equals(
                x.upgradeCoffeeId())) {
            throw bad(
                    "Món nâng cấp phải khác món gốc"
            );
        }

        if (x.priority() < 0
                || x.priority() > 10000) {
            throw bad(
                    "Ưu tiên từ 0 đến 10000"
            );
        }

        if (x.minCartAmount() != null
                && x.minCartAmount().signum() < 0) {
            throw bad(
                    "Giá trị giỏ không được âm"
            );
        }

        if (x.startsAt() != null
                && x.endsAt() != null
                && !x.startsAt().isBefore(x.endsAt())) {
            throw bad(
                    "Thời gian không hợp lệ"
            );
        }

        String type = x.upgradeType() == null
                ? "PREMIUM_VERSION"
                : x.upgradeType();

        if (!"PREMIUM_VERSION".equals(type)) {
            throw bad(
                    "Upsell sản phẩm chỉ hỗ trợ phiên bản cà phê cao cấp hơn"
            );
        }

        Coffee anchor = coffee(x.anchorCoffeeId());
        Coffee upgrade = coffee(x.upgradeCoffeeId());

        if (!isVerifiedVariant(anchor.getId(), upgrade.getId(), type)) {
            throw bad("Cần liên kết hai món khác nhau, giá món nâng cấp cao hơn và có xác nhận cùng gu trải nghiệm.");
        }
        if (upgrade.getPrice()
                .compareTo(anchor.getPrice()) <= 0) {
            throw bad(
                    "Món nâng cấp phải có giá niêm yết cao hơn món gốc"
            );
        }

        String name = x.name() == null
                || x.name().isBlank()
                ? "Nâng cấp - " + anchor.getName()
                : x.name().trim();

        if (name.length() > 150) {
            throw bad(
                    "Tên quy tắc tối đa 150 ký tự"
            );
        }

        r.setName(name);
        r.setAnchorCoffee(anchor);
        r.setUpgradeCoffee(upgrade);
        r.setUpgradeType(type);
        r.setPriority(x.priority());

        r.setMinCartAmount(
                x.minCartAmount() == null
                        ? BigDecimal.ZERO
                        : x.minCartAmount()
        );

        r.setStartsAt(x.startsAt());
        r.setEndsAt(x.endsAt());
        r.setUpdatedAt(LocalDateTime.now());
    }

    // Chuyển dữ liệu Entity sang response cho FE
    private Map<String, Object> output(UpsellRule r) {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("id", r.getId());
        m.put("name", r.getName());

        m.put(
                "anchorCoffeeId",
                r.getAnchorCoffee().getId()
        );

        m.put(
                "anchorName",
                r.getAnchorCoffee().getName()
        );

        m.put(
                "upgradeCoffeeId",
                r.getUpgradeCoffee().getId()
        );

        m.put(
                "upgradeName",
                r.getUpgradeCoffee().getName()
        );

        m.put(
                "upgradePrice",
                r.getUpgradeCoffee().getPrice()
        );

        m.put(
                "basePrice",
                r.getAnchorCoffee().getPrice()
        );

        m.put("upgradeType", r.getUpgradeType());
        List<Map<String, Object>> link = jdbc.queryForList("""
                SELECT experience_note AS "experienceNote"
                FROM public.upsell_variant_links
                WHERE base_coffee_id = ? AND variant_coffee_id = ?
                  AND upgrade_type = ? AND enabled = true
                LIMIT 1
                """, r.getAnchorCoffee().getId(), r.getUpgradeCoffee().getId(), r.getUpgradeType());
        m.put("experienceNote", link.isEmpty() ? null : link.get(0).get("experienceNote"));
        m.put("priority", r.getPriority());
        m.put("minCartAmount", r.getMinCartAmount());
        m.put("startsAt", r.getStartsAt());
        m.put("endsAt", r.getEndsAt());
        m.put("status", r.getStatus());
        m.put("compatible", isVerifiedVariant(r.getAnchorCoffee().getId(),
                r.getUpgradeCoffee().getId(), r.getUpgradeType()));

        m.put(
                "impressions",
                events.countByRuleIdAndEventType(
                        r.getId(),
                        "IMPRESSION"
                )
        );

        m.put(
                "accepts",
                events.countByRuleIdAndEventType(
                        r.getId(),
                        "ACCEPT"
                )
        );

        return m;
    }

    // Danh sách quy tắc
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {

        return rules.findAllByOrderByPriorityDescIdDesc()
                .stream()
                .map(this::output)
                .toList();
    }

    // Tạo quy tắc
    @Transactional
    public Map<String, Object> create(RuleInput x) {

        UpsellRule r = new UpsellRule();

        apply(r, x);

        return output(
                rules.saveAndFlush(r)
        );
    }

    // Chỉnh sửa quy tắc
    @Transactional
    public Map<String, Object> update(
            long id,
            RuleInput x
    ) {

        UpsellRule r = rule(id);

        if ("ACTIVE".equals(r.getStatus())) {
            throw bad(
                    "Tạm dừng trước khi sửa"
            );
        }

        apply(r, x);

        return output(
                rules.saveAndFlush(r)
        );
    }

    // Kích hoạt hoặc tạm dừng quy tắc
    @Transactional
    public Map<String, Object> status(
            long id,
            String status
    ) {

        UpsellRule r = rule(id);

        if (!Set.of(
                "DRAFT",
                "ACTIVE",
                "PAUSED"
        ).contains(status)) {
            throw bad(
                    "Trạng thái quy tắc không hợp lệ"
            );
        }

        if ("ACTIVE".equals(status)) {
            if (!isVerifiedVariant(r.getAnchorCoffee().getId(),
                    r.getUpgradeCoffee().getId(), r.getUpgradeType())) {
                throw bad("Quy tắc cần một món khác, giá cao hơn và có liên kết cùng gu trải nghiệm đã xác nhận.");
            }
            if (r.getUpgradeCoffee().getStatus()
                    != CoffeeStatus.AVAILABLE
                    || r.getUpgradeCoffee()
                    .isAutoSoldOut()) {
                throw bad(
                        "Sản phẩm nâng cấp hiện không bán"
                );
            }
        }

        r.setStatus(status);
        r.setUpdatedAt(LocalDateTime.now());

        return output(
                rules.saveAndFlush(r)
        );
    }

    // Xóa quy tắc
    @Transactional
    public void delete(long id) {

        UpsellRule r = rule(id);

        if ("ACTIVE".equals(r.getStatus())) {
            throw bad(
                    "Tạm dừng trước khi xóa"
            );
        }

        rules.delete(r);
    }

    // Gợi ý món nâng cấp cho khách hàng
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(
            long anchorId,
            BigDecimal total
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        Set<Long> seen = new HashSet<>();

        for (UpsellRule r : rules.eligible(
                anchorId,
                total == null
                        ? BigDecimal.ZERO
                        : total,
                LocalDateTime.now()
        )) {

            Coffee c = r.getUpgradeCoffee();
            if (!isVerifiedVariant(r.getAnchorCoffee().getId(), c.getId(), r.getUpgradeType())) continue;

            if (c.getStatus()
                    != CoffeeStatus.AVAILABLE
                    || c.isAutoSoldOut()
                    || !seen.add(c.getId())) {
                continue;
            }

            Map<String, Object> m = output(r);

            m.put("imageUrl", c.imageSrc());
            m.put(
                    "description",
                    c.getDescription()
            );

            result.add(m);

            if (result.size() == 4) {
                break;
            }
        }

        return result;
    }

    // Xem trước đề xuất
    @Transactional(readOnly = true)
    public List<Map<String, Object>> preview(long id) {
        UpsellRule r = rule(id);
        if (!isVerifiedVariant(r.getAnchorCoffee().getId(), r.getUpgradeCoffee().getId(), r.getUpgradeType())) return List.of();
        return List.of(output(r));
    }

    // =========================================
    // FIX SQLSTATE 23505:
    // GHI SỰ KIỆN UPSELL KHÔNG BỊ TRÙNG
    // =========================================
    @Transactional
    public void event(EventInput x) {

        // 1. Kiểm tra dữ liệu đầu vào
        if (x == null
                || x.ruleId() == null
                || x.eventType() == null) {
            throw bad(
                    "Thiếu thông tin sự kiện"
            );
        }

        // 2. Chỉ cho phép hai loại sự kiện
        if (!Set.of(
                "IMPRESSION",
                "ACCEPT"
        ).contains(x.eventType())) {
            throw bad(
                    "Loại sự kiện không hợp lệ"
            );
        }

        // 3. Kiểm tra UUID
        UUID uuid;

        try {
            uuid = UUID.fromString(
                    x.impressionId()
            );
        } catch (Exception e) {
            throw bad(
                    "impressionId phải là UUID"
            );
        }

        // 4. Kiểm tra quy tắc
        UpsellRule r = rule(x.ruleId());

        if (!"ACTIVE".equals(r.getStatus()) ||
                !isVerifiedVariant(r.getAnchorCoffee().getId(), r.getUpgradeCoffee().getId(), r.getUpgradeType())) {
            throw bad("Quy tắc chưa hoạt động hoặc chưa có phiên bản hợp lệ");
        }

        // 5. Chống ghi trùng bằng PostgreSQL
        //
        // Trước đây:
        // exists() -> save()
        //
        // Hai request đồng thời có thể cùng
        // vượt qua exists() và gây SQLState 23505.
        //
        // Hiện tại:
        // INSERT ... ON CONFLICT DO NOTHING
        //
        // Database tự bỏ qua bản ghi trùng.
        events.insertIfAbsent(
                r.getId(),
                uuid,
                x.eventType()
        );
    }

    // Thống kê hiệu quả Upsell
    @Transactional(readOnly = true)
    public Map<String, Object> analytics(long id) {

        UpsellRule r = rule(id);

        long views =
                events.countByRuleIdAndEventType(
                        r.getId(),
                        "IMPRESSION"
                );

        long accepted =
                events.countByRuleIdAndEventType(
                        r.getId(),
                        "ACCEPT"
                );

        return Map.of(
                "impressions", views,
                "accepts", accepted,
                "acceptRate",
                views == 0
                        ? 0d
                        : (double) accepted / views,
                "purchasesTracked", false,
                "note",
                "ACCEPT chỉ là khách chọn nâng cấp, "
                        + "chưa xác nhận thanh toán thành công."
        );
    }
}
