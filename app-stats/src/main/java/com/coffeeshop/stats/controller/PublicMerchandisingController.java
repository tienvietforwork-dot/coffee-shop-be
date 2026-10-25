package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.dto.response.ProductBundleResponse;
import com.coffeeshop.stats.dto.response.ProductPlacementResponse;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.service.ProductBundleService;
import com.coffeeshop.stats.service.ProductPlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** Public read-only catalog. Admin draft configurations are never exposed. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/merchandising")
public class PublicMerchandisingController {
    private final ProductPlacementService placementService;
    private final ProductBundleService bundleService;

    @GetMapping("/placements")
    public List<ProductPlacementResponse> placements(@RequestParam(required = false) ProductPlacementSection section) {
        return placementService.findPublicRules(section);
    }

    @GetMapping("/bundles")
    public List<ProductBundleResponse> bundles() {
        return bundleService.findPublicBundles();
    }
}
