package com.coffeeshop.controller;

import com.coffeeshop.dto.request.CategoryRequest;
import com.coffeeshop.dto.response.CategoryResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_CATALOG)
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
