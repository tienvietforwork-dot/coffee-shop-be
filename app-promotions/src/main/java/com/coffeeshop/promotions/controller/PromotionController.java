package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.request.PromotionRequest;
import com.coffeeshop.promotions.dto.response.PromotionResponse;
import com.coffeeshop.promotions.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        List<PromotionResponse> responses = promotionService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        PromotionResponse response = promotionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(@RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activatePromotion(@PathVariable Long id) {
        promotionService.activatePromotion(id);
        return ResponseEntity.ok("Kích hoạt chương trình khuyến mãi thành công!");
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<String> pausePromotion(@PathVariable Long id) {
        promotionService.pausePromotion(id);
        return ResponseEntity.ok("Đã tạm ngừng chương trình khuyến mãi và khôi phục giá gốc!");
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<String> resumePromotion(@PathVariable Long id) {
        promotionService.resumePromotion(id);
        return ResponseEntity.ok("Đã tiếp tục chương trình khuyến mãi!");
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<String> endPromotion(@PathVariable Long id) {
        promotionService.endPromotion(id);
        return ResponseEntity.ok("Đã kết thúc chương trình khuyến mãi!");
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<PromotionResponse> clonePromotion(@PathVariable Long id) {
        PromotionResponse response = promotionService.clonePromotion(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}