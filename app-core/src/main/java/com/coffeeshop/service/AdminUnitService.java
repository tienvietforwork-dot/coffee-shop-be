package com.coffeeshop.service;

import com.coffeeshop.entity.VnProvince;
import com.coffeeshop.entity.VnWard;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.VnProvinceRepository;
import com.coffeeshop.repository.VnWardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Danh mục tỉnh / thành → phường / xã (bảng vn_provinces, vn_wards): nguồn cho ô chọn địa chỉ và để kiểm tra địa chỉ giao
 * (phường có thuộc tỉnh không). Danh mục hầu như không đổi nên giữ trong bộ nhớ, đọc lại sau mỗi giờ. Bảng trống (chưa
 * chạy vn_admin_units.sql) thì không kiểm tra gì.
 */
@Service
@RequiredArgsConstructor
public class AdminUnitService {

    private static final long TTL_MS = 60 * 60_000;

    private final VnProvinceRepository provinceRepository;
    private final VnWardRepository wardRepository;

    /** một tỉnh, gọn để gửi cho FE: {@code c} mã, {@code n} tên, {@code w} các phường dạng [mã, tên] */
    public record Unit(int c, String n, List<Object[]> w) {
    }

    /** {@code wards}: key(tỉnh) → key(phường) → tên phường */
    private record Snapshot(List<Unit> units, Map<String, String> provinces, Map<String, Map<String, String>> wards, long at) {
    }

    private volatile Snapshot snapshot;

    public List<Unit> units() {
        return snapshot().units();
    }

    /** tên tỉnh đúng như danh mục (vd. "Hà Nội" → "Thành phố Hà Nội"); không có trong danh mục: BadRequestException */
    public String province(String name) {
        Snapshot s = snapshot();
        if (s.provinces().isEmpty() || name == null) return name;
        String found = s.provinces().get(DeliveryFeeService.key(name));
        if (found == null) throw new BadRequestException("Không có tỉnh / thành phố \"" + name.trim() + "\" trong danh mục");
        return found;
    }

    /** tên phường đúng như danh mục; phường không thuộc tỉnh: BadRequestException */
    public String ward(String ward, String province) {
        Snapshot s = snapshot();
        if (s.provinces().isEmpty() || ward == null) return ward;
        String p = province(province);
        String found = s.wards().getOrDefault(DeliveryFeeService.key(p), Map.of()).get(DeliveryFeeService.key(ward));
        if (found == null) throw new BadRequestException(ward.trim() + " không thuộc " + p);
        return found;
    }

    /** đọc lại danh mục ngay (sau khi sửa bảng) */
    public void reload() {
        snapshot = null;
    }

    private Snapshot snapshot() {
        Snapshot s = snapshot;
        if (s == null || System.currentTimeMillis() - s.at() > TTL_MS) {
            s = load();
            snapshot = s;
        }
        return s;
    }

    private Snapshot load() {
        List<VnProvince> provinces = provinceRepository.findAll().stream().sorted(Comparator.comparing(VnProvince::getCode)).toList();
        Map<Integer, List<VnWard>> byProvince = wardRepository.findAll().stream()
                .sorted(Comparator.comparing(VnWard::getCode))
                .collect(Collectors.groupingBy(VnWard::getProvinceCode, LinkedHashMap::new, Collectors.toList()));
        List<Unit> units = new ArrayList<>();
        Map<String, String> names = new HashMap<>();
        Map<String, Map<String, String>> wards = new HashMap<>();
        for (VnProvince p : provinces) {
            List<VnWard> ws = byProvince.getOrDefault(p.getCode(), List.of());
            units.add(new Unit(p.getCode(), p.getName(), ws.stream().map(w -> new Object[]{w.getCode(), w.getName()}).toList()));
            names.put(DeliveryFeeService.key(p.getName()), p.getName());
            Map<String, String> m = new HashMap<>();
            ws.forEach(w -> m.put(DeliveryFeeService.key(w.getName()), w.getName()));
            wards.put(DeliveryFeeService.key(p.getName()), m);
        }
        return new Snapshot(List.copyOf(units), names, wards, System.currentTimeMillis());
    }
}
