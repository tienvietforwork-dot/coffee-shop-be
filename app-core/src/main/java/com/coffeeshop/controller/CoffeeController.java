package com.coffeeshop.controller;

import com.coffeeshop.dto.request.CoffeeRequest;
import com.coffeeshop.dto.request.CoffeeStatusRequest;
import com.coffeeshop.dto.request.RecipeRequest;
import com.coffeeshop.dto.response.CoffeeResponse;
import com.coffeeshop.dto.response.RecipeResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CoffeeService;
import com.coffeeshop.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CoffeeController {

    private final CoffeeService coffeeService;
    private final RecipeService recipeService;

    @GetMapping("/api/coffees")
    @PreAuthorize(Perm.CAN_READ_CATALOG)
    public List<CoffeeResponse> findAll() {
        return coffeeService.findAll();
    }

    @GetMapping("/api/coffees/{id}")
    @PreAuthorize(Perm.CAN_READ_CATALOG)
    public CoffeeResponse findById(@PathVariable Long id) {
        return coffeeService.findById(id);
    }

    @PostMapping("/api/coffees")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public CoffeeResponse create(@Valid @RequestBody CoffeeRequest request) {
        return coffeeService.create(request);
    }

    @PutMapping("/api/coffees/{id}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    public CoffeeResponse update(@PathVariable Long id, @Valid @RequestBody CoffeeRequest request) {
        return coffeeService.update(id, request);
    }

    /** Bật/tắt hiển thị, còn món / hết món – nhân viên làm được; ngừng bán cần quyền sửa thực đơn (kiểm tra trong service). */
    @PatchMapping("/api/coffees/{id}/status")
    @PreAuthorize(Perm.CAN_MENU_STATUS)
    public CoffeeResponse updateStatus(@PathVariable Long id, @Valid @RequestBody CoffeeStatusRequest request) {
        return coffeeService.updateStatus(id, request.status());
    }

    @DeleteMapping("/api/coffees/{id}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        coffeeService.delete(id);
    }

    @GetMapping("/api/coffees/{id}/recipes")
    @PreAuthorize(Perm.CAN_READ_CATALOG)
    public List<RecipeResponse> recipes(@PathVariable Long id) {
        return recipeService.findByCoffee(id);
    }

    @PostMapping("/api/coffees/{id}/recipes")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse createRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return recipeService.create(id, request);
    }

    @PutMapping("/api/recipes/{recipeId}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    public RecipeResponse updateRecipe(@PathVariable Long recipeId, @Valid @RequestBody RecipeRequest request) {
        return recipeService.update(recipeId, request);
    }

    @PostMapping("/api/recipes/{recipeId}/activate")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    public RecipeResponse activateRecipe(@PathVariable Long recipeId) {
        return recipeService.activate(recipeId);
    }

    @DeleteMapping("/api/recipes/{recipeId}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable Long recipeId) {
        recipeService.delete(recipeId);
    }
}
