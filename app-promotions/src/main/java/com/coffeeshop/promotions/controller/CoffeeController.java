package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.response.CoffeeResponse;
import com.coffeeshop.promotions.service.CoffeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/coffees")
@RequiredArgsConstructor
public class CoffeeController {

    private final CoffeeService coffeeService;

    @GetMapping
    public ResponseEntity<List<CoffeeResponse>> getAll(){
        List<CoffeeResponse> responses = coffeeService.getAll();
        return ResponseEntity.ok(responses);
    }

}
