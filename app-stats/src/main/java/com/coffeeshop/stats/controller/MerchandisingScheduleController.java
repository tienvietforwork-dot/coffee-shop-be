package com.coffeeshop.stats.controller;
import com.coffeeshop.stats.service.MerchandisingScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequiredArgsConstructor
public class MerchandisingScheduleController {
    private final MerchandisingScheduleService service;
    @GetMapping("/api/merchandising-schedules")
    public List<Map<String,Object>> list(){
        return service.list();
    }

    @PostMapping("/api/merchandising-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@RequestBody MerchandisingScheduleService.Input input){
        return service.create(input);
    }

    @PutMapping("/api/merchandising-schedules/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody MerchandisingScheduleService.Input input){
        return service.update(id,input);
    }

    @PostMapping("/api/merchandising-schedules/{id}/publish")
    public Map<String,Object> publish(@PathVariable long id){
        return service.publish(id);
    }

    @PostMapping("/api/merchandising-schedules/{id}/unpublish")
    public Map<String,Object> unpublish(@PathVariable long id){
        return service.unpublish(id);
    }

    @DeleteMapping("/api/merchandising-schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id){
        service.delete(id);
    }

    @GetMapping("/api/merchandising-schedules/{id}/preview")
    public List<Map<String,Object>> preview(@PathVariable long id){
        return service.preview(id);
    }

    @GetMapping("/api/public/merchandising/schedules")
    public List<Map<String,Object>> publicNow(){
        return service.publicNow();
    }
}
