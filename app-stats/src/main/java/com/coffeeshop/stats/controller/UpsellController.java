package com.coffeeshop.stats.controller;
import com.coffeeshop.stats.service.UpsellService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequiredArgsConstructor
public class UpsellController {
    private final UpsellService service;
    @GetMapping("/api/upsell/compatible")
    public List<Map<String,Object>> compatible(@RequestParam Long anchorCoffeeId, @RequestParam String upgradeType) {
        return service.compatible(anchorCoffeeId, upgradeType);
    }

    @GetMapping("/api/upsell/variants")
    public List<Map<String,Object>> variants() { return service.variants(); }

    @PostMapping("/api/upsell/variants")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerVariant(@RequestBody UpsellService.VariantInput x) { service.registerVariant(x); }

    @PostMapping("/api/upsell/variants/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableVariant(@PathVariable long id) { service.deactivateVariant(id); }

    @GetMapping("/api/upsell/rules")
    public List<Map<String,Object>> list(){
        return service.list();
    }

    @PostMapping("/api/upsell/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@RequestBody UpsellService.RuleInput x){
        return service.create(x);
    }

    @PutMapping("/api/upsell/rules/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody UpsellService.RuleInput x){
        return service.update(id,x);
    }

    @PostMapping("/api/upsell/rules/{id}/activate")
    public Map<String,Object> activate(@PathVariable long id){
        return service.status(id,"ACTIVE");
    }

    @PostMapping("/api/upsell/rules/{id}/pause")
    public Map<String,Object> pause(@PathVariable long id){
        return service.status(id,"PAUSED");
    }

    @DeleteMapping("/api/upsell/rules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id){
        service.delete(id);
    }

    @GetMapping("/api/upsell/rules/{id}/preview")
    public List<Map<String,Object>> preview(@PathVariable long id){
        return service.preview(id);
    }

    @GetMapping("/api/upsell/rules/{id}/analytics")
    public Map<String,Object> analytics(@PathVariable long id){
        return service.analytics(id);
    }

    @GetMapping("/api/public/merchandising/upsell")
    public List<Map<String,Object>> suggest(@RequestParam long anchorCoffeeId,@RequestParam(defaultValue="0") BigDecimal cartTotal){
        return service.suggest(anchorCoffeeId,cartTotal);
    }

    @PostMapping("/api/public/merchandising/upsell/events") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void event(@RequestBody UpsellService.EventInput x){
        service.event(x);
    }
}
