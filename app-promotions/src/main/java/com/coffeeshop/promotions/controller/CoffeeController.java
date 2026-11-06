package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.request.CoffeeRequest;
import com.coffeeshop.promotions.dto.response.CoffeeResponse;
import com.coffeeshop.promotions.service.CoffeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<CoffeeResponse> create(@Valid @RequestBody CoffeeRequest request){
        CoffeeResponse response = coffeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
