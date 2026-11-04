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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Nguyên liệu: name + unit only. No stock, batches or movements are tracked. */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        Map<Long, List<MaterialResponse.Usage>> usedIn = new HashMap<>();
        for (var recipe : recipeRepository.findAllActiveWithMaterials()) {
            for (var line : recipe.getMaterials()) {
                usedIn.computeIfAbsent(line.getMaterial().getId(), k -> new ArrayList<>()).add(new MaterialResponse.Usage(
                        "COFFEE", recipe.getCoffee().getId(), recipe.getCoffee().getName(), line.getQuantity()));
            }
        }
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
        Material material = Material.builder().name(name).unit(request.unit().trim()).build();
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
        return MaterialResponse.from(material);
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
}
