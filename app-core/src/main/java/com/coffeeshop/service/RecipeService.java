package com.coffeeshop.service;

import com.coffeeshop.dto.request.RecipeRequest;
import com.coffeeshop.dto.response.RecipeResponse;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Recipe;
import com.coffeeshop.entity.RecipeMaterial;
import com.coffeeshop.entity.RecipeStep;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.RecipeRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final MaterialRepository materialRepository;
    private final CoffeeService coffeeService;
    private final CoffeeStatusBackgroundService coffeeStatusBackgroundService;

    @Transactional(readOnly = true)
    public List<RecipeResponse> findByCoffee(Long coffeeId) {
        return recipeRepository.findByCoffeeIdOrderByVersionDesc(coffeeId).stream().map(RecipeResponse::from).toList();
    }

    /** Recipe in use for every coffee, most recently changed first. */
    @Transactional(readOnly = true)
    public List<RecipeResponse> findAllActive() {
        return recipeRepository.findAllActive().stream().map(RecipeResponse::from)
                .sorted(Comparator.comparing(RecipeResponse::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Creates a new version of the coffee's recipe. */
    @Transactional
    public RecipeResponse create(Long coffeeId, RecipeRequest request) {
        Coffee coffee = coffeeService.get(coffeeId);
        boolean activate = request.activate() == null || request.activate();
        if (activate) deactivateCurrent(coffeeId);
        Recipe recipe = Recipe.builder()
                .coffee(coffee)
                .version(recipeRepository.maxVersion(coffeeId) + 1)
                .active(activate)
                .build();
        apply(recipe, request);
        RecipeResponse saved = RecipeResponse.from(recipeRepository.save(recipe));
        coffeeStatusBackgroundService.requestRun();
        return saved;
    }

    @Transactional
    public RecipeResponse update(Long recipeId, RecipeRequest request) {
        Recipe recipe = get(recipeId);
        softDeleteChildren(recipe);
        recipeRepository.flush(); // old rows must be marked deleted before new ones hit the unique indexes
        apply(recipe, request);
        // children are replaced, so the recipe row itself may not be dirty — stamp it explicitly
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setUpdatedBy(CurrentUser.username());
        if (Boolean.TRUE.equals(request.activate()) && !recipe.isActive()) {
            return activate(recipeId);
        }
        coffeeStatusBackgroundService.requestRun();
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public RecipeResponse activate(Long recipeId) {
        Recipe recipe = get(recipeId);
        deactivateCurrent(recipe.getCoffee().getId());
        recipe.setActive(true);
        coffeeStatusBackgroundService.requestRun();
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public void delete(Long recipeId) {
        Recipe recipe = get(recipeId);
        if (recipe.isActive()) throw new BadRequestException("Không thể xóa công thức đang áp dụng");
        softDeleteChildren(recipe);
        recipe.softDelete();
    }

    private void softDeleteChildren(Recipe recipe) {
        recipe.getMaterials().forEach(RecipeMaterial::softDelete);
        recipe.getMaterials().clear();
        recipe.getSteps().forEach(RecipeStep::softDelete);
        recipe.getSteps().clear();
    }

    private void deactivateCurrent(Long coffeeId) {
        recipeRepository.findByCoffeeIdAndActiveTrue(coffeeId).ifPresent(r -> {
            r.setActive(false);
            recipeRepository.flush(); // partial unique index: only one active recipe per coffee
        });
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.setBrewMethod(request.brewMethod());
        recipe.setDescription(request.description());
        recipe.setBrewTimeMin(request.brewTimeMin());
        Set<Long> seen = new HashSet<>();
        if (request.materials() != null) {
            for (RecipeRequest.Line line : request.materials()) {
                if (!seen.add(line.materialId())) {
                    throw new BadRequestException("Nguyên liệu bị lặp trong công thức: " + line.materialId());
                }
                recipe.getMaterials().add(RecipeMaterial.builder()
                        .recipe(recipe)
                        .material(materialRepository.findById(line.materialId())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu: " + line.materialId())))
                        .quantity(line.quantity())
                        .note(line.note())
                        .build());
            }
        }
        if (request.steps() != null) {
            int no = 1;
            for (String step : request.steps()) {
                if (step == null || step.isBlank()) continue;
                recipe.getSteps().add(RecipeStep.builder().recipe(recipe).stepNo(no++).instruction(step.trim()).build());
            }
        }
    }

    private Recipe get(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức: " + id));
    }
}
