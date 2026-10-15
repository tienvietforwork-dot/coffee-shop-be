package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.dto.request.ProductBundleItemRequest;
import com.coffeeshop.stats.dto.request.ProductBundleRequest;
import com.coffeeshop.stats.dto.response.ProductBundleAnalyticsResponse;
import com.coffeeshop.stats.dto.response.ProductBundleResponse;
import com.coffeeshop.stats.service.ProductBundleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-bundles")
public class ProductBundleController {

    private final ProductBundleService productBundleService;

    @GetMapping
    public List<ProductBundleResponse> findAll() {
        return productBundleService.findAll();
    }

    @GetMapping("/{id}")
    public ProductBundleResponse findById(@PathVariable Long id) {
        return productBundleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBundleResponse create(@Valid @RequestBody ProductBundleRequest request) {
        return productBundleService.create(request);
    }

    @PutMapping("/{id}")
    public ProductBundleResponse update(@PathVariable Long id, @Valid @RequestBody ProductBundleRequest request) {
        return productBundleService.update(id, request);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBundleResponse addItem(@PathVariable Long id, @Valid @RequestBody ProductBundleItemRequest request) {
        return productBundleService.addItem(id, request);
    }

    @PutMapping("/{bundleId}/items/{itemId}")
    public ProductBundleResponse updateItem(@PathVariable Long bundleId, @PathVariable Long itemId,
                                            @Valid @RequestBody ProductBundleItemRequest request) {
        return productBundleService.updateItem(bundleId, itemId, request);
    }

    @DeleteMapping("/{bundleId}/items/{itemId}")
    public ProductBundleResponse removeItem(@PathVariable Long bundleId, @PathVariable Long itemId) {
        return productBundleService.removeItem(bundleId, itemId);
    }

    @PostMapping("/{id}/activate")
    public ProductBundleResponse activate(@PathVariable Long id) {
        return productBundleService.activate(id);
    }

    @PostMapping("/{id}/pause")
    public ProductBundleResponse pause(@PathVariable Long id) {
        return productBundleService.pause(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productBundleService.delete(id);
    }

    @GetMapping("/{id}/analytics")
    public ProductBundleAnalyticsResponse analytics(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return productBundleService.analytics(id, from, to);
    }
}
