package com.coffeeshop.controller;

import com.coffeeshop.service.UpsellModifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/public/upsell-options")
public class PublicUpsellOptionsController {
    private final UpsellModifierService service;
    @GetMapping("/{coffeeId}")
    public List<Map<String,Object>> options(@PathVariable Long coffeeId){ return service.publicOptions(coffeeId); }
}
