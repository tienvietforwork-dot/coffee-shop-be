package com.coffeeshop.service;

import com.coffeeshop.entity.DeliveryFeePlan;
import com.coffeeshop.entity.DeliveryFeeTier;
import com.coffeeshop.entity.DeliverySetting;
import com.coffeeshop.entity.DeliveryTimeSlot;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.distance.DistanceGateway;
import com.coffeeshop.repository.DeliveryFeePlanRepository;
import com.coffeeshop.repository.DeliverySettingRepository;
import com.coffeeshop.repository.DeliveryTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Phí giao hàng (quán tự giao): đo km từ địa chỉ quán tới địa chỉ khách qua API bản đồ, rồi tra khoảng km trong option giá
 * đang áp dụng — option của khung giờ chứa giờ giao, ngoài mọi khung thì option mặc định. Xa hơn khoảng cuối của option
 * = ngoài phạm vi giao; tiền hàng từ "miễn phí ship từ" được miễn phí; dưới "đơn tối thiểu" không giao.
 */
@Service
@RequiredArgsConstructor
public class DeliveryFeeService {

    /** giờ mở cửa / khung giờ tính theo giờ Việt Nam, kể cả khi server chạy UTC */
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DeliverySettingRepository settingRepository;
    private final DeliveryFeePlanRepository planRepository;
    private final DeliveryTimeSlotRepository slotRepository;
    private final DistanceGateway distance;

    public record Settings(String shopAddress, Double shopLat, Double shopLng, LocalTime openTime, LocalTime closeTime,
                           BigDecimal freeShipThreshold, BigDecimal minOrderAmount, String provider) {
    }

    public record Tier(BigDecimal fromKm, BigDecimal toKm, BigDecimal fee) {
    }

    public record Plan(Long id, String name, boolean isDefault, List<Tier> tiers) {
        static Plan from(DeliveryFeePlan p) {
            return new Plan(p.getId(), p.getName(), p.isDefaultPlan(),
                    p.getTiers().stream().map(t -> new Tier(t.getFromKm(), t.getToKm(), t.getFee())).toList());
        }
    }

    /** {@code days}: 1 (thứ 2) … 7 (CN), rỗng = mọi ngày */
    public record Slot(Long id, LocalTime startTime, LocalTime endTime, List<Integer> days, Long planId, String planName) {
        static Slot from(DeliveryTimeSlot s) {
            return new Slot(s.getId(), s.getStartTime(), s.getEndTime(), s.dayList(), s.getPlan().getId(), s.getPlan().getName());
        }
    }

    /** a delivery to one address at one time: how far, which km range of which option, and the fee from what is left to pay */
    public record Rule(BigDecimal distanceKm, Integer minutes, String tierLabel, String planName, Function<BigDecimal, BigDecimal> fee) {
        /** "3–5 km" (option mặc định) or "3–5 km · Cao điểm" */
        public String label() {
            return planName == null ? tierLabel : tierLabel + " · " + planName;
        }
    }

    // ---- cửa hàng ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Settings settings() {
        DeliverySetting s = setting();
        return new Settings(s.getShopAddress(), s.getShopLat(), s.getShopLng(), s.getOpenTime(), s.getCloseTime(),
                s.getFreeShipThreshold(), s.getMinOrderAmount(), distance.provider());
    }

    /** địa chỉ quán đổi (hoặc chưa có toạ độ) thì đo lại toạ độ */
    @Transactional
    public Settings saveSettings(Settings r) {
        if (r.shopAddress() == null || r.shopAddress().isBlank()) throw new BadRequestException("Nhập địa chỉ quán");
        if ((r.openTime() == null) != (r.closeTime() == null) || (r.openTime() != null && r.openTime().equals(r.closeTime()))) {
            throw new BadRequestException("Giờ mở cửa không hợp lệ");
        }
        DeliverySetting s = setting();
        boolean moved = !r.shopAddress().trim().equals(s.getShopAddress());
        s.setShopAddress(r.shopAddress().trim());
        s.setOpenTime(r.openTime());
        s.setCloseTime(r.closeTime());
        s.setFreeShipThreshold(positiveOrNull(r.freeShipThreshold()));
        s.setMinOrderAmount(positiveOrNull(r.minOrderAmount()));
        if (moved || s.getShopLat() == null || s.getShopLng() == null) {
            var p = locate(s.getShopAddress(), null);
            s.setShopLat(p.lat());
            s.setShopLng(p.lng());
        }
        settingRepository.save(s);
        return settings();
    }

    // ---- option giá -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Plan> plans() {
        return planRepository.findAllByOrderByDefaultPlanDescIdAsc().stream().map(Plan::from).toList();
    }

    /** thêm / sửa option giá; option đầu tiên của quán là option mặc định */
    @Transactional
    public Plan savePlan(Long id, Plan r) {
        if (r.name() == null || r.name().isBlank()) throw new BadRequestException("Nhập tên option");
        List<Tier> tiers = validTiers(r.tiers());
        String name = r.name().trim();
        List<DeliveryFeePlan> all = planRepository.findAllByOrderByDefaultPlanDescIdAsc();
        if (all.stream().anyMatch(p -> !p.getId().equals(id) && p.getName().equalsIgnoreCase(name))) {
            throw new BadRequestException("Đã có option tên \"" + name + "\"");
        }
        DeliveryFeePlan p = id == null
                ? DeliveryFeePlan.builder().defaultPlan(all.stream().noneMatch(DeliveryFeePlan::isDefaultPlan)).build()
                : plan(id);
        p.setName(name);
        p.getTiers().clear();
        planRepository.saveAndFlush(p); // orphans go before the new ranges
        for (Tier t : tiers) {
            p.getTiers().add(DeliveryFeeTier.builder().plan(p).fromKm(t.fromKm()).toKm(t.toKm()).fee(t.fee()).build());
        }
        return Plan.from(planRepository.save(p));
    }

    @Transactional
    public void deletePlan(Long id) {
        DeliveryFeePlan p = plan(id);
        if (p.isDefaultPlan()) throw new BadRequestException("Không xóa được option mặc định");
        List<String> used = slotRepository.findAllByOrderByStartTimeAsc().stream().filter(s -> s.getPlan().getId().equals(id))
                .map(s -> s.getStartTime() + "–" + s.getEndTime()).toList();
        if (!used.isEmpty()) throw new BadRequestException("Option đang dùng ở khung giờ " + String.join(", ", used));
        planRepository.delete(p);
    }

    // ---- khung giờ -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Slot> slots() {
        return slotRepository.findAllByOrderByStartTimeAsc().stream().map(Slot::from).toList();
    }

    /** thêm / sửa khung giờ; 2 khung không được trùng giờ trong cùng một ngày */
    @Transactional
    public Slot saveSlot(Long id, Slot r) {
        if (r.startTime() == null || r.endTime() == null || r.startTime().equals(r.endTime())) {
            throw new BadRequestException("Giờ bắt đầu và kết thúc không hợp lệ");
        }
        if (r.planId() == null) throw new BadRequestException("Chọn option giá");
        DeliveryTimeSlot s = id == null ? new DeliveryTimeSlot() : slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ: " + id));
        List<Integer> days = r.days() == null ? List.of()
                : r.days().stream().filter(d -> d != null && d >= 1 && d <= 7).distinct().sorted().toList();
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setDays(days.isEmpty() || days.size() == 7 ? null : String.join(",", days.stream().map(String::valueOf).toList()));
        s.setPlan(plan(r.planId()));
        for (DeliveryTimeSlot other : slotRepository.findAllByOrderByStartTimeAsc()) {
            if (other.getId().equals(s.getId())) continue;
            LocalTime clash = overlap(s, other);
            if (clash != null) {
                throw new BadRequestException("Trùng với khung " + other.getStartTime() + "–" + other.getEndTime() + " (lúc " + clash + ")");
            }
        }
        return Slot.from(slotRepository.save(s));
    }

    @Transactional
    public void deleteSlot(Long id) {
        slotRepository.deleteById(id);
    }

    // ---- tính phí ------------------------------------------------------------------

    /**
     * Fee rule for a delivery to {@code address} at {@code at} (giờ giao; null = bây giờ, giờ Việt Nam), for PricingService.
     * No address yet: null (PricingService then shows no fee for a quote). Out of range or below the minimum: BadRequestException.
     */
    @Transactional(readOnly = true)
    public Rule rule(String address, LocalDateTime at) {
        if (address == null || address.isBlank()) return null;
        DeliverySetting s = setting();
        LocalDateTime when = at != null ? at : LocalDateTime.now(SHOP_ZONE);
        DeliveryFeePlan plan = slotRepository.findAllByOrderByStartTimeAsc().stream()
                .filter(x -> x.covers(when.getDayOfWeek(), when.toLocalTime())).map(DeliveryTimeSlot::getPlan).findFirst()
                .orElseGet(() -> planRepository.findFirstByDefaultPlanTrue().orElse(null));
        if (plan == null || plan.getTiers().isEmpty()) throw new BadRequestException("Quán chưa thiết lập phí giao hàng");
        DistanceGateway.Point shop = shopPoint(s);
        DistanceGateway.Route route;
        try {
            route = distance.route(shop, distance.geocode(address, shop));
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        BigDecimal km = BigDecimal.valueOf(route.km()).setScale(2, RoundingMode.HALF_UP);
        List<DeliveryFeeTier> tiers = plan.getTiers().stream().sorted(Comparator.comparing(DeliveryFeeTier::getFromKm)).toList();
        DeliveryFeeTier tier = tiers.stream().filter(t -> km.compareTo(t.getToKm()) <= 0).findFirst().orElse(null);
        if (tier == null) {
            throw new BadRequestException("Địa chỉ cách quán " + km.setScale(1, RoundingMode.HALF_UP).toPlainString()
                    + " km — ngoài phạm vi giao (tối đa " + tiers.get(tiers.size() - 1).getToKm().stripTrailingZeros().toPlainString() + " km)");
        }
        BigDecimal free = s.getFreeShipThreshold();
        BigDecimal min = s.getMinOrderAmount();
        BigDecimal fee = tier.getFee();
        return new Rule(km, route.minutes(), tier.label(), plan.isDefaultPlan() ? null : plan.getName(), amount -> {
            if (min != null && amount.compareTo(min) < 0) {
                throw new BadRequestException("Đơn giao hàng tối thiểu " + min.stripTrailingZeros().toPlainString() + "đ");
            }
            if (free != null && amount.compareTo(free) >= 0) return BigDecimal.ZERO;
            return fee;
        });
    }

    /** gợi ý địa chỉ khi gõ, ưu tiên quanh quán */
    @Transactional(readOnly = true)
    public List<String> suggest(String query, String ward, String province) {
        DeliverySetting s = setting();
        DistanceGateway.Point near = s.getShopLat() == null ? null : new DistanceGateway.Point(s.getShopLat(), s.getShopLng());
        return distance.suggest(query, near, ward, province);
    }

    /**
     * Giờ mở cửa: {@code at} (giờ hẹn nhận / giao) phải trong giờ mở cửa; không hẹn giờ ({@code at} null) thì quán phải đang
     * mở. Dùng cho đơn khách tự đặt (QR / online).
     */
    @Transactional(readOnly = true)
    public void checkOpen(LocalDateTime at) {
        DeliverySetting s = setting();
        if (s.getOpenTime() == null) return;
        String hours = s.getOpenTime() + "–" + s.getCloseTime();
        if (at != null) {
            if (!isOpen(s, at.toLocalTime())) throw new BadRequestException("Giờ hẹn ngoài giờ mở cửa (" + hours + ")");
        } else if (!isOpen(s, LocalTime.now(SHOP_ZONE))) {
            throw new BadRequestException("Quán đang đóng cửa (mở " + hours + ") — vui lòng chọn giờ hẹn nhận");
        }
    }

    // ---- helpers ---------------------------------------------------------------

    private static boolean isOpen(DeliverySetting s, LocalTime t) {
        LocalTime o = s.getOpenTime(), c = s.getCloseTime();
        return c.isAfter(o) ? !t.isBefore(o) && t.isBefore(c) : !t.isBefore(o) || t.isBefore(c);
    }

    /** first minute (of any day) both slots cover, or null */
    private static LocalTime overlap(DeliveryTimeSlot a, DeliveryTimeSlot b) {
        for (DayOfWeek day : DayOfWeek.values()) {
            for (int m = 0; m < 1440; m++) {
                LocalTime t = LocalTime.of(m / 60, m % 60);
                if (a.covers(day, t) && b.covers(day, t)) return t;
            }
        }
        return null;
    }

    private DistanceGateway.Point shopPoint(DeliverySetting s) {
        if (s.getShopLat() != null && s.getShopLng() != null) return new DistanceGateway.Point(s.getShopLat(), s.getShopLng());
        return locate(s.getShopAddress(), null);
    }

    private DistanceGateway.Point locate(String address, DistanceGateway.Point near) {
        try {
            return distance.geocode(address, near);
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /** sorted, non-overlapping, each from < to, fees ≥ 0 */
    private static List<Tier> validTiers(List<Tier> in) {
        if (in == null || in.isEmpty()) throw new BadRequestException("Thêm ít nhất 1 khoảng km");
        List<Tier> tiers = new ArrayList<>(in);
        for (Tier t : tiers) {
            if (t == null || t.fromKm() == null || t.toKm() == null || t.fee() == null) throw new BadRequestException("Nhập đủ từ km, đến km và phí");
            if (t.fromKm().signum() < 0 || t.toKm().compareTo(t.fromKm()) <= 0) {
                throw new BadRequestException("Khoảng km không hợp lệ: " + t.fromKm() + "–" + t.toKm());
            }
            if (t.fee().signum() < 0) throw new BadRequestException("Phí ship không hợp lệ");
        }
        tiers.sort(Comparator.comparing(Tier::fromKm));
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).fromKm().compareTo(tiers.get(i - 1).toKm()) < 0) {
                throw new BadRequestException("Các khoảng km bị chồng nhau: " + tiers.get(i - 1).toKm().stripTrailingZeros().toPlainString()
                        + " > " + tiers.get(i).fromKm().stripTrailingZeros().toPlainString());
            }
        }
        return tiers;
    }

    private static BigDecimal positiveOrNull(BigDecimal v) {
        return v == null || v.signum() <= 0 ? null : v;
    }

    private DeliveryFeePlan plan(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy option giá: " + id));
    }

    /** the single settings row; defaults (saved on the first save) when missing */
    private DeliverySetting setting() {
        return settingRepository.findById(DeliverySetting.ID).orElseGet(() -> DeliverySetting.builder()
                .id(DeliverySetting.ID).shopAddress("Chợ Bến Thành, Quận 1, TP. Hồ Chí Minh").shopLat(10.7725).shopLng(106.6981).build());
    }
}
