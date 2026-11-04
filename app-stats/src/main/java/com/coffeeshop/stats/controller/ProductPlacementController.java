package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.dto.request.CategoryPriorityRequest;
import com.coffeeshop.stats.dto.request.CoffeeVisibilityRequest;
import com.coffeeshop.stats.dto.request.ProductPlacementRequest;
import com.coffeeshop.stats.dto.response.CategoryPlacementResponse;
import com.coffeeshop.stats.dto.response.CoffeePlacementResponse;
import com.coffeeshop.stats.dto.response.ProductPlacementOverviewResponse;
import com.coffeeshop.stats.dto.response.ProductPlacementResponse;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.service.ProductPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-placement")
public class ProductPlacementController {

    private final ProductPlacementService productPlacementService;
    private final com.coffeeshop.stats.service.ProductPlacementInsightsService insightsService;

    @GetMapping("/suggestions")
    public List<com.coffeeshop.stats.service.ProductPlacementInsightsService.Suggestion> suggestions(
            @RequestParam ProductPlacementSection section, @RequestParam(defaultValue = "8") int limit) {
        return insightsService.suggest(section, limit);
    }

    @GetMapping("/overview")
    public ProductPlacementOverviewResponse overview() {
        return productPlacementService.overview();
    }

    @GetMapping("/rules")
    public List<ProductPlacementResponse> findRules(
            @RequestParam(required = false) ProductPlacementSection section,
            @RequestParam(defaultValue = "false") boolean visibleOnly) {
        return productPlacementService.findRules(section, visibleOnly);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductPlacementResponse create(@Valid @RequestBody ProductPlacementRequest request) {
        return productPlacementService.create(request);
    }

    @PutMapping("/rules/{id}")
    public ProductPlacementResponse update(@PathVariable Long id, @Valid @RequestBody ProductPlacementRequest request) {
        return productPlacementService.update(id, request);
    }

    @PostMapping("/rules/{id}/publish")
    public ProductPlacementResponse publish(@PathVariable Long id) {
        return productPlacementService.publish(id);
    }

    @PostMapping("/rules/{id}/unpublish")
    public ProductPlacementResponse unpublish(@PathVariable Long id) {
        return productPlacementService.unpublish(id);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productPlacementService.delete(id);
    }

    @PatchMapping("/coffees/{id}/visibility")
    public CoffeePlacementResponse updateCoffeeVisibility(@PathVariable Long id,
                                                          @Valid @RequestBody CoffeeVisibilityRequest request) {
        return productPlacementService.updateCoffeeVisibility(id, request);
    }

    @PatchMapping("/categories/{id}/priority")
    public CategoryPlacementResponse updateCategoryPriority(@PathVariable Long id,
                                                            @Valid @RequestBody CategoryPriorityRequest request) {
        return productPlacementService.updateCategoryPriority(id, request);
    }
}
