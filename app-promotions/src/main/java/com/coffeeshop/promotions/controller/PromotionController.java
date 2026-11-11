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
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/promotions")
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        List<PromotionResponse> responses = promotionService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/promotions/{id}")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        PromotionResponse response = promotionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/promotions")
    public ResponseEntity<PromotionResponse> createPromotion(@RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/promotions/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promotions/{id}/active")
    public ResponseEntity<String> activatePromotion(@PathVariable Long id) {
        promotionService.activePromotion(id);
        return ResponseEntity.ok("Kích hoạt chương trình khuyến mãi thành công!");
    }

    @PostMapping("/promotions/{id}/pause")
    public ResponseEntity<String> pausePromotion(@PathVariable Long id) {
        promotionService.pausePromotion(id);
        return ResponseEntity.ok("Đã tạm ngừng chương trình khuyến mãi và khôi phục giá gốc!");
    }



    @PostMapping("/promotions/{id}/end")
    public ResponseEntity<String> endPromotion(@PathVariable Long id) {
        promotionService.endPromotion(id);
        return ResponseEntity.ok("Đã kết thúc chương trình khuyến mãi!");
    }

    @PostMapping("/promotions/{id}/clone")
    public ResponseEntity<PromotionResponse> clonePromotion(@PathVariable Long id) {
        PromotionResponse response = promotionService.clonePromotion(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/promotions/{id}/remove")
    public ResponseEntity<Void> removePromotion(@PathVariable Long id){
        promotionService.removePromotion(id);
        return ResponseEntity.noContent().build();
    }
}