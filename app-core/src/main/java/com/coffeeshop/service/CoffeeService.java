package com.coffeeshop.service;

import com.coffeeshop.dto.request.CoffeeRequest;
import com.coffeeshop.dto.response.CoffeeResponse;
import com.coffeeshop.dto.response.MenuResponse;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CategoryRepository;
import com.coffeeshop.repository.CoffeeRepository;
import com.coffeeshop.repository.PromotionRepository;
import com.coffeeshop.repository.RecipeRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.security.Perm;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoffeeService {

    private final CoffeeRepository coffeeRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final RecipeRepository recipeRepository;
    private final PromotionRepository promotionRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public List<CoffeeResponse> findAll() {
        return coffeeRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CoffeeResponse findById(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public CoffeeResponse create(CoffeeRequest request) {
        Coffee coffee = Coffee.builder().build();
        apply(coffee, request);
        return toResponse(coffeeRepository.save(coffee));
    }

    @Transactional
    public CoffeeResponse update(Long id, CoffeeRequest request) {
        Coffee coffee = get(id);
        apply(coffee, request);
        return toResponse(coffee);
    }

    @Transactional
    public CoffeeResponse updateStatus(Long id, CoffeeStatus status) {
        Coffee coffee = get(id);
        if ((status == CoffeeStatus.DISCONTINUED || coffee.getStatus() == CoffeeStatus.DISCONTINUED)
                && !CurrentUser.principal().getPermissions().contains(Perm.MENU_EDIT)) {
            throw new AccessDeniedException("Cần quyền sửa thực đơn để ngừng bán / bán lại món");
        }
        coffee.setStatus(status);
        return toResponse(coffee);
    }

    @Transactional
    public void delete(Long id) {
        get(id).softDelete();
    }

    /** Public menu: AVAILABLE and SOLD_OUT coffees are shown, HIDDEN / DISCONTINUED are not. */
    @Transactional(readOnly = true)
    public MenuResponse menu() {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<Coffee>> byCategory = coffeeRepository
                .findByStatusInOrderByNameAsc(List.of(CoffeeStatus.AVAILABLE, CoffeeStatus.SOLD_OUT)).stream()
                .collect(Collectors.groupingBy(c -> c.getCategory().getId()));
        var coffeePromos = pricingService.runningCoffeePromotions(now);

        List<MenuResponse.MenuCategory> categories = new ArrayList<>();
        for (var category : categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()) {
            List<Coffee> coffees = byCategory.getOrDefault(category.getId(), List.of());
            if (coffees.isEmpty()) continue;
            categories.add(new MenuResponse.MenuCategory(category.getId(), category.getName(),
                    category.getDescription(), coffees.stream().map(c -> {
                        var best = pricingService.bestCoffeePrice(c, 1, coffeePromos.getOrDefault(c.getId(), List.of()));
                        return new MenuResponse.MenuCoffee(c.getId(), c.getName(), c.getImageUrl(), c.getDescription(),
                                c.getPrice(), best == null ? null : best.price(),
                                best == null ? null : best.item().getPromotion().getName(),
                                c.getStatus() == CoffeeStatus.AVAILABLE);
                    }).toList()));
        }
        var orderPromos = promotionRepository.findRunningOrderPromotions(now).stream()
                .map(p -> new MenuResponse.OrderPromotion(p.getId(), p.getName(), p.getDescription(),
                        p.getDiscountPercent(), p.getMinOrderAmount(), p.getEndDate()))
                .toList();
        return new MenuResponse(categories, orderPromos);
    }

    Coffee get(Long id) {
        return coffeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cà phê: " + id));
    }

    private CoffeeResponse toResponse(Coffee coffee) {
        return CoffeeResponse.from(coffee, recipeRepository.findByCoffeeIdAndActiveTrue(coffee.getId()).isPresent());
    }

    private void apply(Coffee coffee, CoffeeRequest request) {
        coffee.setCategory(categoryService.get(request.categoryId()));
        coffee.setName(request.name().trim());
        coffee.setImageUrl(request.imageUrl());
        coffee.setPrice(request.price());
        coffee.setDescription(request.description());
        if (request.status() != null) coffee.setStatus(request.status());
    }
}
