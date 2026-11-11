package com.coffeeshop.service;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nguyên liệu thô: tên, đơn vị, số lượng hiện có. Không có phiếu nhập / xuất hay lịch sử —
 * nhân viên sửa thẳng số lượng hoặc điều chỉnh nhanh (+/−). Đơn bắt đầu pha tự trừ (StockService).
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final RecipeRepository recipeRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        Map<Long, List<MaterialResponse.Usage>> usedIn = usage();
        return materialRepository.findAllByOrderByNameAsc().stream()
                .map(m -> MaterialResponse.from(m, usedIn.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        String name = request.name().trim();
        if (materialRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + name);
        }
        Material material = Material.builder().name(name).unit(request.unit().trim())
                .quantity(request.quantity() == null ? BigDecimal.ZERO : request.quantity()).build();
        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = get(id);
        String name = request.name().trim();
        if (!material.getName().equalsIgnoreCase(name) && materialRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + name);
        }
        material.setName(name);
        material.setUnit(request.unit().trim());
        if (request.quantity() != null) material.setQuantity(request.quantity());
        stockService.refreshAvailability();
        return withUsage(material);
    }

    /** Cộng / trừ nhanh số lượng; không cho xuống dưới 0. */
    @Transactional
    public MaterialResponse adjust(Long id, BigDecimal delta) {
        Material material = get(id);
        BigDecimal next = material.getQuantity().add(delta);
        if (next.signum() < 0) {
            throw new BadRequestException("Số lượng sau điều chỉnh không được âm (hiện có "
                    + material.getQuantity().stripTrailingZeros().toPlainString() + " " + material.getUnit() + ")");
        }
        material.setQuantity(next);
        stockService.refreshAvailability();
        return withUsage(material);
    }

    /** A material still named in a recipe (any version) cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        Material material = get(id);
        if (materialRepository.isUsedInRecipe(id)) {
            throw new BadRequestException("Nguyên liệu đang có trong công thức, không thể xóa: " + material.getName());
        }
        material.softDelete();
    }

    Material get(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu: " + id));
    }

    private MaterialResponse withUsage(Material m) {
        return MaterialResponse.from(m, usage().getOrDefault(m.getId(), List.of()));
    }

    private Map<Long, List<MaterialResponse.Usage>> usage() {
        Map<Long, List<MaterialResponse.Usage>> usedIn = new HashMap<>();
        for (var recipe : recipeRepository.findAllActiveWithMaterials()) {
            for (var line : recipe.getMaterials()) {
                usedIn.computeIfAbsent(line.getMaterial().getId(), k -> new ArrayList<>()).add(new MaterialResponse.Usage(
                        "COFFEE", recipe.getCoffee().getId(), recipe.getCoffee().getName(), line.getQuantity()));
            }
        }
        return usedIn;
    }
}
