package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.request.VoucherDetailRequest;
import com.coffeeshop.promotions.dto.request.VoucherRequest;
import com.coffeeshop.promotions.dto.response.VoucherResponse;
import com.coffeeshop.promotions.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> getAll(){
        return ResponseEntity.ok(voucherService.getAll());
    }

    @PostMapping("/vouchers")
    public ResponseEntity<VoucherResponse> create(@Valid @RequestBody VoucherRequest request){
        VoucherResponse response = voucherService.create(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/vouchers/{id}")
    public ResponseEntity<VoucherResponse> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request){
        VoucherResponse response = voucherService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/vouchers/{id}/remove")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        voucherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/vouchers/{id}/voucher-detail")
    public  ResponseEntity<VoucherResponse> createVoucherDetail(@PathVariable Long id, @Valid @RequestBody VoucherDetailRequest request){
        VoucherResponse response = voucherService.createVoucherDetail(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vouchers/{id}/active")
    public ResponseEntity<String> activeVoucher(@PathVariable Long id){
        voucherService.activeVoucher(id);
        return ResponseEntity.ok("Kích hoạt chương trình voucher");
    }

    @PostMapping("/vouchers/{id}/end")
    public ResponseEntity<String> endVoucher(@PathVariable Long id){
        voucherService.endVoucher(id);
        return ResponseEntity.ok("Kết thúc chương trình khuyến mãi");
    }

    @PostMapping("/vouchers/{id}/clone")
    public ResponseEntity<VoucherResponse> cloneVoucher(@PathVariable Long id){
        VoucherResponse response = voucherService.cloneVoucher(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
