package com.coffeeshop.service;

import com.coffeeshop.entity.DeliveryFeePlan;
import com.coffeeshop.entity.DeliveryFeeTier;
import com.coffeeshop.entity.DeliverySetting;
import com.coffeeshop.entity.DeliveryTimeSlot;
import com.coffeeshop.entity.DeliveryWeatherTier;
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
    private final com.coffeeshop.integration.weather.WeatherGateway weather;
    private final com.coffeeshop.repository.DeliveryWeatherTierRepository weatherTierRepository;

    public record Settings(String shopAddress, Double shopLat, Double shopLng, LocalTime openTime, LocalTime closeTime,
                           BigDecimal freeShipThreshold, BigDecimal minOrderAmount, BigDecimal minutesPerKm, String provider) {
    }

    /** mức km: từ {@code fromKm} km trở đi → {@code fee}; {@code toKm} (tự tính: mốc kế tiếp hoặc maxKm) chỉ để đọc */
    public record Tier(BigDecimal fromKm, BigDecimal toKm, BigDecimal fee) {
    }

    /**
     * Option giá: các mức "từ X km → phí" và {@code maxKm} = không giao quá … km. {@code note}: ghi chú cho khách khi
     * option này được áp dụng (trống = câu mặc định).
     */
    public record Plan(Long id, String name, boolean isDefault, String note, List<Tier> tiers, BigDecimal maxKm) {
        static Plan from(DeliveryFeePlan p) {
            return new Plan(p.getId(), p.getName(), p.isDefaultPlan(), p.getNote(),
                    p.getTiers().stream().map(t -> new Tier(t.getFromKm(), t.getToKm(), t.getFee())).toList(),
                    p.getTiers().stream().map(DeliveryFeeTier::getToKm).max(Comparator.naturalOrder()).orElse(null));
        }
    }

    /** {@code days}: 1 (thứ 2) … 7 (CN), rỗng = mọi ngày */
    public record Slot(Long id, LocalTime startTime, LocalTime endTime, List<Integer> days, Long planId, String planName) {
        static Slot from(DeliveryTimeSlot s) {
            return new Slot(s.getId(), s.getStartTime(), s.getEndTime(), s.dayList(), s.getPlan().getId(), s.getPlan().getName());
        }
    }

    /**
     * a delivery to one address at one time: how far, which km range of which option, the bad-weather surcharge
     * ({@code weather} = "Mưa 3,2 mm/h" when charged) and the fee from what is left to pay
     */
    public record Rule(BigDecimal distanceKm, Integer minutes, String tierLabel, String planName, String weather,
                       BigDecimal weatherSurcharge, String note, Function<BigDecimal, BigDecimal> fee) {
        /** "3–5 km", "3–5 km · Cao điểm", "3–5 km · Cao điểm · Mưa 3,2 mm/h" */
        public String label() {
            return String.join(" · ", java.util.stream.Stream.of(tierLabel, planName, weather).filter(x -> x != null).toList());
        }
    }

    /** bậc phụ phí: mưa từ {@code minRainMm} mm/h → +{@code surcharge} */
    public record RainTier(BigDecimal minRainMm, BigDecimal surcharge) {
    }

    /**
     * thời tiết xấu: bật / tắt, bậc phụ phí theo lượng mưa, ngưng giao khi mưa từ … mm/h (có giông thì luôn ngưng giao).
     * {@code outlook}: thời tiết tại quán bây giờ + vài giờ tới (chỉ đọc).
     */
    public record WeatherSettings(boolean enabled, List<RainTier> rainTiers, BigDecimal stopRainMm, String note, List<WeatherHour> outlook) {
    }

    /** thời tiết một giờ (đầu danh sách = hiện tại) và quán sẽ làm gì: phụ phí bao nhiêu, có ngưng giao không */
    public record WeatherHour(LocalDateTime time, int code, String description, double temperature, double rainMm,
                              double windKmh, BigDecimal surcharge, boolean stopped) {
    }

    // ---- cửa hàng ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Settings settings() {
        DeliverySetting s = setting();
        return new Settings(s.getShopAddress(), s.getShopLat(), s.getShopLng(), s.getOpenTime(), s.getCloseTime(),
                s.getFreeShipThreshold(), s.getMinOrderAmount(), s.getMinutesPerKm(), distance.provider());
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
        s.setMinutesPerKm(positiveOrNull(r.minutesPerKm()));
        if (moved || s.getShopLat() == null || s.getShopLng() == null) {
            var p = locate(s.getShopAddress(), null);
            s.setShopLat(p.lat());
            s.setShopLng(p.lng());
        }
        settingRepository.save(s);
        return settings();
    }

    // ---- thời tiết xấu ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public WeatherSettings weatherSettings() {
        DeliverySetting s = setting();
        List<DeliveryWeatherTier> tiers = weatherTierRepository.findAllByOrderByMinRainMmAsc();
        List<WeatherHour> outlook = s.getShopLat() == null ? List.of() : weather.outlook(s.getShopLat(), s.getShopLng(), 6).stream()
                .map(w -> new WeatherHour(w.time(), w.code(), w.describe(), round1(w.temperature()), round1(w.rainMmPerHour()),
                        round1(w.windKmh()), surchargeFor(s, tiers, w), stops(s, w))).toList();
        return new WeatherSettings(s.isWeatherEnabled(), tiers.stream().map(t -> new RainTier(t.getMinRainMm(), t.getSurcharge())).toList(),
                s.getWeatherStopRainMm(), s.getWeatherNote(), outlook);
    }

    @Transactional
    public WeatherSettings saveWeather(WeatherSettings r) {
        List<RainTier> tiers = r.rainTiers() == null ? List.of() : r.rainTiers().stream().filter(java.util.Objects::nonNull).toList();
        for (RainTier t : tiers) {
            if (t.minRainMm() == null || t.minRainMm().signum() <= 0) throw new BadRequestException("Lượng mưa phải lớn hơn 0");
            if (t.surcharge() == null || t.surcharge().signum() < 0) throw new BadRequestException("Nhập phụ phí");
        }
        if (tiers.stream().map(t -> t.minRainMm().stripTrailingZeros()).distinct().count() < tiers.size()) {
            throw new BadRequestException("Hai bậc mưa trùng lượng mưa");
        }
        DeliverySetting s = setting();
        s.setWeatherEnabled(r.enabled());
        s.setWeatherStopRainMm(positiveOrNull(r.stopRainMm()));
        s.setWeatherNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        settingRepository.save(s);
        weatherTierRepository.deleteAllInBatch();
        weatherTierRepository.saveAll(tiers.stream().map(t -> DeliveryWeatherTier.builder().minRainMm(t.minRainMm()).surcharge(t.surcharge()).build()).toList());
        return weatherSettings();
    }

    /** phụ phí lúc thời tiết {@code w}: bậc mưa cao nhất đạt; 0 = không */
    private static BigDecimal surchargeFor(DeliverySetting s, List<DeliveryWeatherTier> tiers, com.coffeeshop.integration.weather.WeatherGateway.Weather w) {
        if (!s.isWeatherEnabled() || w == null) return BigDecimal.ZERO;
        BigDecimal rain = tiers.stream().filter(t -> w.rainMmPerHour() >= t.getMinRainMm().doubleValue())
                .map(DeliveryWeatherTier::getSurcharge).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return rain;
    }

    /** mưa từ ngưỡng ngưng giao, hoặc có giông (giông thì không ai đi giao) */
    private static boolean stops(DeliverySetting s, com.coffeeshop.integration.weather.WeatherGateway.Weather w) {
        if (!s.isWeatherEnabled() || w == null) return false;
        return w.storm() || (s.getWeatherStopRainMm() != null && w.rainMmPerHour() >= s.getWeatherStopRainMm().doubleValue());
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // ---- option giá -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Plan> plans() {
        return planRepository.findAllByOrderByDefaultPlanDescIdAsc().stream().map(Plan::from).toList();
    }

    /**
     * Thêm / sửa option giá. {@code isDefault} = đặt làm option mặc định (option mặc định cũ bỏ cờ). Luôn có đúng 1 option
     * mặc định: option đầu tiên tự là mặc định, và không bỏ cờ mặc định của chính nó được — phải đặt option khác.
     */
    @Transactional
    public Plan savePlan(Long id, Plan r) {
        if (r.name() == null || r.name().isBlank()) throw new BadRequestException("Nhập tên option");
        List<Tier> tiers = validTiers(r.tiers(), r.maxKm());
        String name = r.name().trim();
        List<DeliveryFeePlan> all = planRepository.findAllByOrderByDefaultPlanDescIdAsc();
        if (all.stream().anyMatch(p -> !p.getId().equals(id) && p.getName().equalsIgnoreCase(name))) {
            throw new BadRequestException("Đã có option tên \"" + name + "\"");
        }
        DeliveryFeePlan p = id == null ? new DeliveryFeePlan() : plan(id);
        boolean makeDefault = r.isDefault() || all.stream().noneMatch(DeliveryFeePlan::isDefaultPlan);
        if (p.isDefaultPlan() && !makeDefault) {
            throw new BadRequestException("Phải luôn có 1 option mặc định — đặt option khác làm mặc định trước");
        }
        if (makeDefault && !p.isDefaultPlan()) {
            // only one default (unique index): clear the old one first
            all.stream().filter(DeliveryFeePlan::isDefaultPlan).forEach(x -> x.setDefaultPlan(false));
            planRepository.flush();
            p.setDefaultPlan(true);
        }
        p.setName(name);
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
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
        if (p.isDefaultPlan()) throw new BadRequestException("Không xóa được option mặc định — đặt option khác làm mặc định trước");
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
        // khung giờ phải nằm trong giờ mở cửa
        DeliverySetting shop = setting();
        if (shop.getOpenTime() != null) {
            for (LocalTime t = r.startTime(); !t.equals(r.endTime()); t = t.plusMinutes(1)) {
                if (!isOpen(shop, t)) {
                    throw new BadRequestException("Khung giờ phải nằm trong giờ mở cửa (" + shop.getOpenTime() + "–" + shop.getCloseTime() + ")");
                }
            }
        }
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
        // thời tiết xấu: ngưng giao, hoặc cộng phụ phí vào mọi khung giờ (kể cả đơn được miễn phí ship theo tiền hàng)
        var w = s.isWeatherEnabled() ? weather.at(shop.lat(), shop.lng(), at) : null;
        if (stops(s, w)) throw new BadRequestException("Thời tiết xấu (" + w.describe() + ") — quán tạm ngưng giao hàng");
        BigDecimal extra = surchargeFor(s, weatherTierRepository.findAllByOrderByMinRainMmAsc(), w);
        boolean bad = extra.signum() > 0;
        // ghi chú cho khách: vì sao phí khác thường (option của khung giờ, thời tiết xấu)
        List<String> notes = new ArrayList<>();
        if (!plan.isDefaultPlan()) notes.add(plan.getNote() != null ? plan.getNote() : "Phí giao hàng khung giờ " + plan.getName());
        if (bad) notes.add(s.getWeatherNote() != null ? s.getWeatherNote() : "Phụ phí do thời tiết xấu (" + w.describe() + ")");
        return new Rule(km, route.minutes(), tier.label(), plan.isDefaultPlan() ? null : plan.getName(),
                bad ? w.describe() : null, bad ? extra : null, notes.isEmpty() ? null : String.join(". ", notes), amount -> {
            if (min != null && amount.compareTo(min) < 0) {
                throw new BadRequestException("Đơn giao hàng tối thiểu " + min.stripTrailingZeros().toPlainString() + "đ");
            }
            if (free != null && amount.compareTo(free) >= 0) return extra;
            return fee.add(extra);
        });
    }

    /** kiểm tra phường đã chọn: {@code outside} = ngay điểm gần nhất của phường cũng xa hơn khoảng km xa nhất quán giao */
    public record AreaCheck(boolean outside, Double nearestKm, BigDecimal maxKm) {
    }

    @Transactional(readOnly = true)
    public AreaCheck checkArea(String ward, String province) {
        BigDecimal max = planRepository.findAllByOrderByDefaultPlanDescIdAsc().stream().flatMap(p -> p.getTiers().stream())
                .map(DeliveryFeeTier::getToKm).max(Comparator.naturalOrder()).orElse(null);
        DeliverySetting s = setting();
        Double km = max == null ? null : distance.nearestKm(ward, province, shopPoint(s));
        Double rounded = km == null ? null : Math.round(km * 10) / 10.0;
        return new AreaCheck(km != null && km > max.doubleValue(), rounded, max);
    }

    /** quãng đường đi xe quán → một địa chỉ; {@code outside} = xa hơn mức "không giao quá" lớn nhất của các option */
    public record Distance(BigDecimal km, boolean outside, BigDecimal maxKm) {
    }

    @Transactional(readOnly = true)
    public Distance distanceTo(String address) {
        if (address == null || address.isBlank()) throw new BadRequestException("Nhập địa chỉ");
        BigDecimal max = planRepository.findAllByOrderByDefaultPlanDescIdAsc().stream().flatMap(p -> p.getTiers().stream())
                .map(DeliveryFeeTier::getToKm).max(Comparator.naturalOrder()).orElse(null);
        DistanceGateway.Point shop = shopPoint(setting());
        DistanceGateway.Route route;
        try {
            route = distance.route(shop, distance.geocode(address, shop));
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        BigDecimal km = BigDecimal.valueOf(route.km()).setScale(1, RoundingMode.HALF_UP);
        return new Distance(km, max != null && km.compareTo(max) > 0, max);
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

    /**
     * Mức "từ X km → phí" (mức đầu từ 0 km, không trùng mốc) + không giao quá {@code maxKm}: trả về các khoảng
     * [từ, mốc kế tiếp) với khoảng cuối kết thúc ở maxKm. maxKm trống = "đến km" lớn nhất gửi lên (API cũ).
     */
    private static List<Tier> validTiers(List<Tier> in, BigDecimal maxKm) {
        if (in == null || in.isEmpty()) throw new BadRequestException("Thêm ít nhất 1 mức km");
        List<Tier> tiers = new ArrayList<>(in.stream().filter(java.util.Objects::nonNull).toList());
        for (Tier t : tiers) {
            if (t.fromKm() == null || t.fee() == null) throw new BadRequestException("Nhập đủ km và phí");
            if (t.fromKm().signum() < 0) throw new BadRequestException("Km không hợp lệ");
            if (t.fee().signum() < 0) throw new BadRequestException("Phí ship không hợp lệ");
        }
        tiers.sort(Comparator.comparing(Tier::fromKm));
        if (tiers.get(0).fromKm().signum() != 0) throw new BadRequestException("Mức đầu tiên phải từ 0 km");
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).fromKm().compareTo(tiers.get(i - 1).fromKm()) == 0) throw new BadRequestException("Hai mức trùng km");
        }
        BigDecimal max = maxKm != null ? maxKm
                : tiers.stream().map(Tier::toKm).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        if (max == null) throw new BadRequestException("Nhập \"Không giao quá … km\"");
        BigDecimal last = tiers.get(tiers.size() - 1).fromKm();
        if (max.compareTo(last) <= 0) {
            throw new BadRequestException("\"Không giao quá\" phải lớn hơn " + last.stripTrailingZeros().toPlainString() + " km");
        }
        List<Tier> out = new ArrayList<>();
        for (int i = 0; i < tiers.size(); i++) {
            BigDecimal to = i + 1 < tiers.size() ? tiers.get(i + 1).fromKm() : max;
            out.add(new Tier(tiers.get(i).fromKm(), to, tiers.get(i).fee()));
        }
        return out;
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
