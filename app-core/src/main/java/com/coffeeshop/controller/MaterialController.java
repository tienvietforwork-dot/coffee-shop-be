package com.coffeeshop.controller;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_MATERIALS)
    public List<MaterialResponse> materials() {
        return materialService.findAll();
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@Valid @RequestBody MaterialRequest request) {
        return materialService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return materialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Perm.CAN_MATERIALS_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        materialService.delete(id);
    }
}
