package com.coffeeshop.service;

import com.coffeeshop.dto.request.CategoryRequest;
import com.coffeeshop.dto.response.CategoryResponse;
import com.coffeeshop.entity.Category;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CategoryRepository;
import com.coffeeshop.repository.CoffeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CoffeeRepository coffeeRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Danh mục đã tồn tại: " + request.name());
        }
        Category category = Category.builder().build();
        apply(category, request);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = get(id);
        if (!category.getName().equalsIgnoreCase(request.name()) && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Danh mục đã tồn tại: " + request.name());
        }
        apply(category, request);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        if (coffeeRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Không thể xóa danh mục đang có cà phê");
        }
        get(id).softDelete();
    }

    Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + id));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    }
}
