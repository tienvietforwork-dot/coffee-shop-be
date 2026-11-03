package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.service.CrossSellService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequiredArgsConstructor
public class CrossSellController {
    private final CrossSellService service;
    @GetMapping("/api/cross-sell/rules")
    public List<Map<String,Object>> list(){
        return service.rules();
    }

    @GetMapping("/api/cross-sell/rules/{id}")
    public Map<String,Object> one(@PathVariable long id){
        return service.one(id);
    }

    @PostMapping("/api/cross-sell/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@RequestBody CrossSellService.RuleInput input){
        return service.create(input);
    }

    @PutMapping("/api/cross-sell/rules/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody CrossSellService.RuleInput input){
        return service.update(id,input);
    }

    @PostMapping("/api/cross-sell/rules/{id}/activate")
    public Map<String,Object> activate(@PathVariable long id){
        return service.status(id,"ACTIVE");
    }

    @PostMapping("/api/cross-sell/rules/{id}/pause")
    public Map<String,Object> pause(@PathVariable long id){
        return service.status(id,"PAUSED");
    }

    @DeleteMapping("/api/cross-sell/rules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id){
        service.delete(id);
    }

    @GetMapping("/api/cross-sell/rules/{id}/preview")
    public List<Map<String,Object>> preview(@PathVariable long id){
        return service.preview(id);
    }

    @GetMapping("/api/cross-sell/rules/{id}/analytics")
    public Map<String,Object> analytics(@PathVariable long id){
        return service.analytics(id);
    }

    @GetMapping("/api/public/merchandising/cross-sell")
    public List<Map<String,Object>> publicSuggestions(@RequestParam long anchorCoffeeId,@RequestParam(defaultValue="0") BigDecimal cartTotal,@RequestParam(defaultValue="4") int limit){
        return service.recommend(anchorCoffeeId,cartTotal,limit);
    }

    @PostMapping("/api/public/merchandising/cross-sell/events") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordEvent(@RequestBody CrossSellService.EventInput event){
        service.event(event);
    }
}
