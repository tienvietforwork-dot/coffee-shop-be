package com.coffeeshop.stats.controller;

import com.coffeeshop.stats.service.UpsellOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upsell/options")
@RequiredArgsConstructor
public class UpsellOptionController {
    private final UpsellOptionService service;
    @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) Long coffeeId){
        return service.list(coffeeId);
    }

    @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void create(@RequestBody UpsellOptionService.OptionInput input){
        service.create(input);
    }

    @PutMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id,@RequestBody UpsellOptionService.OptionInput input){
        service.update(id,input);
    }

    @GetMapping("/{id}/ingredients")
    public List<Map<String,Object>> ingredients(@PathVariable long id){
        return service.ingredients(id);
    }

    @PutMapping("/{id}/ingredients") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingredients(@PathVariable long id,@RequestBody List<UpsellOptionService.Ingredient> body){
        service.setIngredients(id,body);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id){
        service.delete(id);
    }

    @PostMapping("/{id}/disable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable long id){
        service.disable(id);
    }
}
