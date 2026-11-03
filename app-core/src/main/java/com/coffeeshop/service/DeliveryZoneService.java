package com.coffeeshop.service;

import com.coffeeshop.entity.DeliveryZone;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Phí giao hàng theo khu vực (how Sapo / KiotViet web order do it, rather than per km): the customer picks the quận,
 * the zone covering it gives the fee; tiền hàng from the zone's threshold ships free; below its minimum it is not delivered.
 */
@Service
@RequiredArgsConstructor
public class DeliveryZoneService {

    private final DeliveryZoneRepository repository;

    public record ZoneRequest(String name, List<String> districts, BigDecimal fee, BigDecimal freeShipThreshold,
                              BigDecimal minOrderAmount, Integer estimatedMinutes, Boolean active, Integer sortOrder) {
    }

    @Transactional(readOnly = true)
    public List<DeliveryZone> findAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<DeliveryZone> active() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().filter(DeliveryZone::isActive).toList();
    }

    @Transactional
    public DeliveryZone save(Long id, ZoneRequest r) {
        if (r.name() == null || r.name().isBlank()) throw new BadRequestException("Nhập tên khu vực");
        if (r.districts() == null || r.districts().stream().allMatch(d -> d == null || d.isBlank())) {
            throw new BadRequestException("Chọn ít nhất 1 quận / phường");
        }
        if (r.fee() == null || r.fee().signum() < 0) throw new BadRequestException("Phí giao hàng không hợp lệ");
        DeliveryZone z = id == null ? new DeliveryZone() : get(id);
        List<String> districts = r.districts().stream().filter(d -> d != null && !d.isBlank()).map(String::trim).distinct().toList();
        // a district belongs to one active zone only
        Map<String, String> taken = new HashMap<>();
        for (DeliveryZone other : active()) {
            if (other.getId().equals(z.getId())) continue;
            other.districtList().forEach(d -> taken.put(key(d), other.getName()));
        }
        if (r.active() == null || r.active()) {
            for (String d : districts) {
                if (taken.containsKey(key(d))) throw new BadRequestException("'" + d + "' đã thuộc khu vực " + taken.get(key(d)));
            }
        }
        z.setName(r.name().trim());
        z.setDistricts(String.join(", ", districts));
        z.setFee(r.fee());
        z.setFreeShipThreshold(r.freeShipThreshold());
        z.setMinOrderAmount(r.minOrderAmount());
        z.setEstimatedMinutes(r.estimatedMinutes());
        z.setActive(r.active() == null || r.active());
        z.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        return repository.save(z);
    }

    @Transactional
    public void delete(Long id) {
        get(id).softDelete();
    }

    /** the active zone covering a district, or null */
    @Transactional(readOnly = true)
    public DeliveryZone zoneOf(String district) {
        if (district == null || district.isBlank()) return null;
        String k = key(district);
        return active().stream().filter(z -> z.districtList().stream().anyMatch(d -> key(d).equals(k))).findFirst().orElse(null);
    }

    /**
     * Fee rule for a delivery to {@code district}, for PricingService: amount after discounts → phí ship.
     * Outside every zone, or below the zone's minimum, the order cannot be delivered.
     */
    public Function<BigDecimal, BigDecimal> feeRule(String district) {
        if (district == null || district.isBlank()) return null;
        DeliveryZone z = zoneOf(district);
        if (z == null) throw new BadRequestException("'" + district + "' ngoài khu vực giao hàng của quán");
        return amount -> {
            if (z.getMinOrderAmount() != null && amount.compareTo(z.getMinOrderAmount()) < 0) {
                throw new BadRequestException("Đơn giao tới " + district + " tối thiểu "
                        + z.getMinOrderAmount().stripTrailingZeros().toPlainString() + "đ");
            }
            if (z.getFreeShipThreshold() != null && amount.compareTo(z.getFreeShipThreshold()) >= 0) return BigDecimal.ZERO;
            return z.getFee();
        };
    }

    private DeliveryZone get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực: " + id));
    }

    /** "Quận 1" = "quan 1" = "Q1"-insensitive enough: accents, case and spaces ignored */
    private static String key(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
