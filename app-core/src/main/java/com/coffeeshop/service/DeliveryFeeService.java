package com.coffeeshop.service;

import com.coffeeshop.entity.DeliveryProvince;
import com.coffeeshop.entity.DeliverySetting;
import com.coffeeshop.entity.DeliveryWard;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.distance.DistanceGateway;
import com.coffeeshop.repository.DeliveryProvinceRepository;
import com.coffeeshop.repository.DeliverySettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phí giao hàng theo khu vực (quán tự giao): chỉ giao tới các tỉnh / thành đã cấu hình (mặc định Hà Nội), mỗi tỉnh một phí
 * ship; phường / xã có thể đặt phí riêng hoặc chặn không giao. Tiền hàng từ "miễn phí ship từ" được miễn phí; khách thuộc
 * nhóm CRM được chọn (Thân thiết, VIP…) luôn được miễn phí; dưới "đơn tối thiểu" không giao.
 * <p>
 * Địa chỉ giao là chuỗi "số nhà đường, Phường …, Tỉnh/Thành phố …" (form chọn tỉnh → phường) — hai phần cuối là phường
 * và tỉnh. Quãng đường (km, phút) vẫn đo nếu được, chỉ để ước tính giờ giao và vẽ tuyến, không ảnh hưởng phí.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFeeService {

    /** giờ mở cửa tính theo giờ Việt Nam, kể cả khi server chạy UTC */
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DeliverySettingRepository settingRepository;
    private final DeliveryProvinceRepository provinceRepository;
    /** bảng nhóm khách do app-crm quản lý (không có entity ở app-core); JDBC để lỗi không làm hỏng giao dịch JPA */
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;
    private final DistanceGateway distance;
    private final AdminUnitService adminUnits;

    /** độ chính xác của toạ độ: đúng địa chỉ, chỉ tới trung tâm phường / xã, hay chỉ tới trung tâm tỉnh */
    public enum Precision { ADDRESS, WARD, PROVINCE }

    public record Located(DistanceGateway.Point point, Precision precision) {
    }

    /** {@code freeShipGroupIds}: nhóm khách CRM được miễn phí ship; {@code groups}: mọi nhóm CRM để chọn (chỉ đọc) */
    public record Settings(String shopAddress, Double shopLat, Double shopLng, LocalTime openTime, LocalTime closeTime,
                           BigDecimal freeShipThreshold, BigDecimal minOrderAmount, BigDecimal speedKmh,
                           List<Long> freeShipGroupIds, List<Group> groups, String provider) {
    }

    public record Group(Long id, String name) {
    }

    /** phường / xã khai báo riêng: {@code fee} null = theo phí tỉnh; {@code blocked} = không giao */
    public record Ward(String name, BigDecimal fee, boolean blocked) {
    }

    public record Area(Long id, String name, BigDecimal fee, boolean active, List<Ward> wards) {
        static Area from(DeliveryProvince p) {
            return new Area(p.getId(), p.getName(), p.getFee(), p.isActive(),
                    p.getWards().stream().map(w -> new Ward(w.getName(), w.getFee(), w.isBlocked())).toList());
        }
    }

    /**
     * Một lần giao tới một địa chỉ: khu vực ("Phường Ba Đình, Thành phố Hà Nội"), quãng đường đo được (có thể null),
     * ghi chú cho khách (miễn phí vì là khách VIP…) và phí tính từ tiền hàng còn phải trả.
     */
    public record Rule(BigDecimal distanceKm, Integer minutes, String zone, String note, Function<BigDecimal, BigDecimal> fee) {
        public String label() {
            return zone;
        }
    }

    // ---- cửa hàng ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Settings settings() {
        DeliverySetting s = setting();
        return new Settings(s.getShopAddress(), s.getShopLat(), s.getShopLng(), s.getOpenTime(), s.getCloseTime(),
                s.getFreeShipThreshold(), s.getMinOrderAmount(), s.getSpeedKmh(), s.freeShipGroups(), groups(), distance.provider());
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
        s.setSpeedKmh(positiveOrNull(r.speedKmh()));
        List<Long> ids = r.freeShipGroupIds() == null ? List.of() : r.freeShipGroupIds().stream().filter(Objects::nonNull).distinct().toList();
        s.setFreeShipGroupIds(ids.isEmpty() ? null : ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        if (moved || s.getShopLat() == null || s.getShopLng() == null) {
            try {
                var p = distance.geocode(s.getShopAddress(), null);
                s.setShopLat(p.lat());
                s.setShopLng(p.lng());
            } catch (RuntimeException e) {
                throw new BadRequestException("Không tìm được vị trí địa chỉ quán: " + e.getMessage());
            }
        }
        settingRepository.save(s);
        return settings();
    }

    private List<Group> groups() {
        try {
            return jdbc.query("select id, name from customer_groups where not del_flag order by name",
                    (rs, i) -> new Group(rs.getLong(1), rs.getString(2)));
        } catch (RuntimeException e) { // bảng CRM chưa có (môi trường thử)
            return List.of();
        }
    }

    // ---- khu vực giao ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Area> areas() {
        return provinceRepository.findAllByOrderByNameAsc().stream().map(Area::from).toList();
    }

    /** thêm / sửa một tỉnh cùng danh sách phường có phí riêng / không giao */
    @Transactional
    public Area saveArea(Long id, Area r) {
        if (r.name() == null || r.name().isBlank()) throw new BadRequestException("Chọn tỉnh / thành phố");
        if (r.fee() == null || r.fee().signum() < 0) throw new BadRequestException("Nhập phí giao hàng của tỉnh");
        String name = adminUnits.province(r.name().trim());
        for (DeliveryProvince other : provinceRepository.findAllByOrderByNameAsc()) {
            if (!other.getId().equals(id) && key(other.getName()).equals(key(name))) {
                throw new BadRequestException("Đã cấu hình " + other.getName());
            }
        }
        DeliveryProvince p = id == null ? new DeliveryProvince() : province(id);
        p.setName(name);
        p.setFee(r.fee());
        p.setActive(r.active());
        p.getWards().clear();
        provinceRepository.saveAndFlush(p);
        Set<String> seen = new HashSet<>();
        for (Ward w : r.wards() == null ? List.<Ward>of() : r.wards()) {
            if (w == null || w.name() == null || w.name().isBlank()) continue;
            if (!seen.add(key(w.name()))) throw new BadRequestException("Phường / xã bị trùng: " + w.name());
            if (!w.blocked() && w.fee() == null) continue; // không khác tỉnh: không cần lưu
            if (w.fee() != null && w.fee().signum() < 0) throw new BadRequestException("Phí không hợp lệ: " + w.name());
            p.getWards().add(DeliveryWard.builder().province(p).name(adminUnits.ward(w.name().trim(), name)).fee(w.blocked() ? null : w.fee()).blocked(w.blocked()).build());
        }
        return Area.from(provinceRepository.save(p));
    }

    @Transactional
    public void deleteArea(Long id) {
        provinceRepository.delete(province(id));
    }

    /** kết quả kiểm tra một phường / xã: giao được không, phí bao nhiêu, vì sao không giao */
    public record AreaCheck(boolean outside, BigDecimal fee, String zone, String message) {
    }

    @Transactional(readOnly = true)
    public AreaCheck checkArea(String ward, String province) {
        try {
            Zone z = zone(ward, province);
            return new AreaCheck(false, z.fee(), z.label(), null);
        } catch (BadRequestException e) {
            return new AreaCheck(true, null, null, e.getMessage());
        }
    }

    // ---- tính phí ------------------------------------------------------------------

    /**
     * Fee rule for a delivery to {@code address} for PricingService; {@code customerId} (có thể null) để xét miễn phí ship
     * theo nhóm khách. No address yet: null. Outside the delivery area or below the minimum: BadRequestException.
     */
    @Transactional(readOnly = true)
    public Rule rule(String address, Long customerId) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.split(",");
        if (parts.length < 3) throw new BadRequestException("Địa chỉ giao cần đủ số nhà / đường, phường / xã, tỉnh / thành phố");
        Zone zone = zone(parts[parts.length - 2], parts[parts.length - 1]);
        DeliverySetting s = setting();
        List<String> freeGroups = freeShipGroupsOf(s, customerId);
        BigDecimal km = null;
        Integer minutes = null;
        try { // quãng đường chỉ để ước tính giờ giao / vẽ tuyến; không thấy số nhà thì đo tới trung tâm phường
            DistanceGateway.Point shop = shopPoint(s);
            Located to = locate(address, shop);
            if (to != null) {
                var route = distance.route(shop, to.point());
                km = BigDecimal.valueOf(route.km()).setScale(2, RoundingMode.HALF_UP);
                minutes = route.minutes();
            }
        } catch (RuntimeException e) {
            log.debug("Không đo được quãng đường tới {}: {}", address, e.getMessage());
        }
        BigDecimal free = s.getFreeShipThreshold();
        BigDecimal min = s.getMinOrderAmount();
        String note = freeGroups.isEmpty() ? null : "Miễn phí giao hàng cho khách " + String.join(", ", freeGroups);
        return new Rule(km, minutes, zone.label(), note, amount -> {
            if (min != null && amount.compareTo(min) < 0) {
                throw new BadRequestException("Đơn giao hàng tối thiểu " + min.stripTrailingZeros().toPlainString() + "đ");
            }
            if (!freeGroups.isEmpty()) return BigDecimal.ZERO;
            if (free != null && amount.compareTo(free) >= 0) return BigDecimal.ZERO;
            return zone.fee();
        });
    }

    /** nhóm khách được miễn phí ship mà khách đang thuộc (rỗng = không) */
    private List<String> freeShipGroupsOf(DeliverySetting s, Long customerId) {
        List<Long> ids = s.freeShipGroups();
        if (customerId == null || ids.isEmpty()) return List.of();
        try {
            return jdbc.queryForList("""
                    select g.name from customer_group_members m join customer_groups g on g.id = m.group_id
                    where m.customer_id = :c and m.group_id in (:ids) and not m.del_flag and not m.is_excluded and not g.del_flag
                    order by g.name""", java.util.Map.of("c", customerId, "ids", ids), String.class);
        } catch (RuntimeException e) {
            log.warn("Không đọc được nhóm khách CRM: {}", e.getMessage());
            return List.of();
        }
    }

    private record Zone(String ward, String province, BigDecimal fee) {
        String label() {
            return ward + ", " + province;
        }
    }

    private Zone zone(String ward, String province) {
        if (province == null || province.isBlank()) throw new BadRequestException("Chọn tỉnh / thành phố");
        if (ward == null || ward.isBlank()) throw new BadRequestException("Chọn phường / xã");
        List<DeliveryProvince> all = provinceRepository.findAllByOrderByNameAsc();
        List<String> allowed = all.stream().filter(DeliveryProvince::isActive).map(DeliveryProvince::getName).toList();
        if (allowed.isEmpty()) throw new BadRequestException("Quán chưa thiết lập khu vực giao hàng");
        DeliveryProvince p = all.stream().filter(x -> x.isActive() && key(x.getName()).equals(key(province))).findFirst()
                .orElseThrow(() -> new BadRequestException("Quán chỉ giao trong khu vực " + String.join(", ", allowed)));
        adminUnits.ward(ward, p.getName()); // phường phải thuộc tỉnh theo danh mục
        DeliveryWard w = p.getWards().stream().filter(x -> key(x.getName()).equals(key(ward))).findFirst().orElse(null);
        if (w != null && w.isBlocked()) throw new BadRequestException("Quán chưa giao tới " + w.getName().trim());
        return new Zone(ward.trim(), p.getName(), w != null && w.getFee() != null ? w.getFee() : p.getFee());
    }

    /** "Thành phố Hà Nội" = "TP. Hà Nội" = "ha noi": bỏ dấu, tiền tố hành chính, chữ hoa (giống unitKey ở FE) */
    static String key(String s) {
        String k = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase();
        return k.replaceFirst("^(thanh pho|tp\\.?|tinh|phuong|xa|dac khu|thi tran)\\s+", "")
                .replaceAll("\\bhcm\\b", "ho chi minh").replaceAll("\\s+", " ").trim();
    }

    /** gợi ý địa chỉ khi gõ, ưu tiên quanh quán */
    @Transactional(readOnly = true)
    public List<String> suggest(String query, String ward, String province) {
        DeliverySetting s = setting();
        DistanceGateway.Point near = s.getShopLat() == null ? null : new DistanceGateway.Point(s.getShopLat(), s.getShopLng());
        return distance.suggest(query, near, ward, province);
    }

    /**
     * Toạ độ một địa chỉ giao (bản đồ tuyến giao). Khách gõ số nhà / đường tuỳ ý nên bản đồ có thể không biết: khi đó lấy
     * trung tâm phường / xã, rồi tới trung tâm tỉnh (đánh dấu {@link Precision}). Không tìm được gì: null.
     */
    public Located locateOrNull(String address) {
        DeliverySetting s = setting();
        return locate(address, s.getShopLat() == null ? null : new DistanceGateway.Point(s.getShopLat(), s.getShopLng()));
    }

    private Located locate(String address, DistanceGateway.Point near) {
        if (address == null || address.isBlank()) return null;
        List<String> segs = java.util.Arrays.stream(address.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
        record Try(String q, Precision precision) {
        }
        List<Try> tries = new java.util.ArrayList<>();
        if (segs.size() >= 3) tries.add(new Try(address, Precision.ADDRESS));
        if (segs.size() >= 2) tries.add(new Try(String.join(", ", segs.subList(segs.size() - 2, segs.size())), Precision.WARD));
        if (!segs.isEmpty()) tries.add(new Try(segs.get(segs.size() - 1), Precision.PROVINCE));
        for (Try t : tries) {
            try {
                return new Located(distance.geocode(t.q(), near), t.precision());
            } catch (RuntimeException e) {
                log.debug("Không định vị được {}: {}", t.q(), e.getMessage());
            }
        }
        return null;
    }

    /** toạ độ quán (điểm xuất phát tuyến giao) */
    @Transactional(readOnly = true)
    public DistanceGateway.Point shop() {
        return shopPoint(setting());
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

    /** giờ {@code t} có trong giờ mở cửa không (chưa đặt giờ mở cửa = luôn mở) */
    @Transactional(readOnly = true)
    public boolean isOpenAt(LocalTime t) {
        DeliverySetting s = setting();
        return s.getOpenTime() == null || isOpen(s, t);
    }

    // ---- helpers ---------------------------------------------------------------

    private static boolean isOpen(DeliverySetting s, LocalTime t) {
        LocalTime o = s.getOpenTime(), c = s.getCloseTime();
        return c.isAfter(o) ? !t.isBefore(o) && t.isBefore(c) : !t.isBefore(o) || t.isBefore(c);
    }

    private DistanceGateway.Point shopPoint(DeliverySetting s) {
        if (s.getShopLat() != null && s.getShopLng() != null) return new DistanceGateway.Point(s.getShopLat(), s.getShopLng());
        try {
            return distance.geocode(s.getShopAddress(), null);
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static BigDecimal positiveOrNull(BigDecimal v) {
        return v == null || v.signum() <= 0 ? null : v;
    }

    private DeliveryProvince province(Long id) {
        return provinceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực: " + id));
    }

    /** the single settings row; defaults (saved on the first save) when missing */
    private DeliverySetting setting() {
        return settingRepository.findById(DeliverySetting.ID).orElseGet(() -> DeliverySetting.builder()
                .id(DeliverySetting.ID).shopAddress("Hoàn Kiếm, Thành phố Hà Nội").shopLat(21.0285).shopLng(105.8542).build());
    }
}
