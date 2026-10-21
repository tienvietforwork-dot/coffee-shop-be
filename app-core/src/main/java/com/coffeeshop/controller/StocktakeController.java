package com.coffeeshop.controller;

import com.coffeeshop.dto.request.StocktakeRequest;
import com.coffeeshop.dto.response.StocktakeResponse;
import com.coffeeshop.entity.enums.StocktakeStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Phiếu kiểm kê kho. */
@RestController
@RequestMapping("/api/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    public List<StocktakeResponse> search(@RequestParam(required = false) StocktakeStatus status) {
        return stocktakeService.search(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    public StocktakeResponse find(@PathVariable Long id) {
        return stocktakeService.find(id);
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    @ResponseStatus(HttpStatus.CREATED)
    public StocktakeResponse create(@Valid @RequestBody StocktakeRequest request) {
        return stocktakeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    public StocktakeResponse update(@PathVariable Long id, @Valid @RequestBody StocktakeRequest request) {
        return stocktakeService.update(id, request);
    }

    /** body: { "resolution": "ý kiến giải quyết số chênh lệch" } */
    @PostMapping("/{id}/balance")
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    public StocktakeResponse balance(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return stocktakeService.balance(id, body == null ? null : body.get("resolution"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Perm.CAN_STOCKTAKE)
    public StocktakeResponse cancel(@PathVariable Long id) {
        return stocktakeService.cancel(id);
    }
}
