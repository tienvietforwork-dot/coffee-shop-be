package com.coffeeshop.stats.controller;
import com.coffeeshop.stats.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService service;
    @GetMapping("/api/recommendation/rules")
    public List<Map<String,Object>> list(){
        return service.list();
    }

    @PostMapping("/api/recommendation/rules")
    @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody RecommendationService.Input x){
        return service.create(x);
    }

    @PutMapping("/api/recommendation/rules/{id}")
    public Map<String,Object> update(@PathVariable Long id,@RequestBody RecommendationService.Input x){
        return service.update(id,x);
    }

    @PostMapping("/api/recommendation/rules/{id}/activate")
    public Map<String,Object> activate(@PathVariable Long id){
        return service.status(id,"ACTIVE");
    }

    @PostMapping("/api/recommendation/rules/{id}/pause")
    public Map<String,Object> pause(@PathVariable Long id){
        return service.status(id,"PAUSED");
    }

    @DeleteMapping("/api/recommendation/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        service.delete(id);
    }

    @GetMapping("/api/recommendation/rules/{id}/analytics")
    public Map<String,Object> analytics(@PathVariable Long id){
        return service.analytics(id);
    }

    @GetMapping("/api/recommendation/rules/{id}/preview")
    public List<Map<String,Object>> preview(@PathVariable Long id,@RequestParam Long anchorCoffeeId){
        return service.recommend(anchorCoffeeId,12,true).stream().filter(x->x.get("ruleId").equals(id)).toList();
    }

    @GetMapping("/api/public/merchandising/recommendation")
    public List<Map<String,Object>> recommend(@RequestParam Long anchorCoffeeId,@RequestParam(defaultValue="4") int limit){
        return service.recommend(anchorCoffeeId,limit,false);
    }

    @PostMapping("/api/public/merchandising/recommendation/events")
    @ResponseStatus(HttpStatus.NO_CONTENT) public void event(@RequestBody RecommendationService.EventInput x){
        service.event(x);
    }
}
