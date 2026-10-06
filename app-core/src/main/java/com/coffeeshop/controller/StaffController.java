package com.coffeeshop.controller;

import com.coffeeshop.dto.request.StaffRequest;
import com.coffeeshop.dto.response.StaffResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @PreAuthorize(Perm.CAN_READ_STAFF)
    public List<StaffResponse> findAll() {
        return staffService.findAll();
    }

    @PostMapping
    @PreAuthorize(Perm.CAN_STAFF)
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@Valid @RequestBody StaffRequest request) {
        return staffService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perm.CAN_STAFF)
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return staffService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Perm.CAN_STAFF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        staffService.delete(id);
    }
}
