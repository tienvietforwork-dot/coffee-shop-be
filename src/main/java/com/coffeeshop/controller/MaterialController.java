package com.coffeeshop.controller;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.request.MaterialTransactionRequest;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public List<MaterialResponse> findAll() {
        return materialService.findAll();
    }

    @GetMapping("/low-stock")
    public List<MaterialResponse> lowStock() {
        return materialService.findLowStock();
    }

    @GetMapping("/{id}")
    public MaterialResponse findById(@PathVariable Long id) {
        return materialService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@Valid @RequestBody MaterialRequest request) {
        return materialService.create(request);
    }

    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return materialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transactions")
    public MaterialResponse recordTransaction(@Valid @RequestBody MaterialTransactionRequest request,
                                               Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : null;
        return materialService.recordTransaction(request, actor);
    }
}
