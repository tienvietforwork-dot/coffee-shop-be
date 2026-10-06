package com.coffeeshop.controller;

import com.coffeeshop.dto.request.DiningTableRequest;
import com.coffeeshop.dto.response.DiningTableResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.DiningTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService tableService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_TABLES)
    public List<DiningTableResponse> findAll() {
        return tableService.findAll();
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_TABLES_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public DiningTableResponse create(@Valid @RequestBody DiningTableRequest request) {
        return tableService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_TABLES_EDIT)
    public DiningTableResponse update(@PathVariable Long id, @Valid @RequestBody DiningTableRequest request) {
        return tableService.update(id, request);
    }

    @PostMapping("/{id}/regenerate-qr")
    @PreAuthorize(Perm.CAN_TABLES_EDIT)
    public DiningTableResponse regenerateQr(@PathVariable Long id) {
        return tableService.regenerateQr(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Perm.CAN_TABLES_EDIT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tableService.delete(id);
    }
}
