package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.request.VoucherRequest;
import com.coffeeshop.promotions.dto.response.VoucherResponse;
import com.coffeeshop.promotions.entity.Voucher;
import com.coffeeshop.promotions.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getAll(){
        return ResponseEntity.ok(voucherService.getAll());
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> create(@Valid @RequestBody VoucherRequest request){
        VoucherResponse response = voucherService.create(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("{id}")
    public ResponseEntity<VoucherResponse> update(@Valid @PathVariable Long id, @RequestBody VoucherRequest request){
        VoucherResponse response = voucherService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        voucherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{id}")
    public  ResponseEntity<VoucherResponse> createVoucherDetail(){

    }
}
