package com.coffeeshop.promotions.controller;

import com.coffeeshop.promotions.dto.request.VoucherDetailRequest;
import com.coffeeshop.promotions.dto.request.VoucherRequest;
import com.coffeeshop.promotions.dto.request.VoucherTemplateRequest;
import com.coffeeshop.promotions.dto.response.*;
import com.coffeeshop.promotions.entity.CustomerGroup;
import com.coffeeshop.promotions.entity.VoucherTemplates;
import com.coffeeshop.promotions.service.VoucherService;
import com.coffeeshop.promotions.service.VoucherTemplateService;
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
    private final VoucherTemplateService voucherTemplateService;

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
    public  ResponseEntity<VoucherResponse> createVoucherDetail(@PathVariable Long id, @Valid @RequestBody List<VoucherDetailRequest> request){
        VoucherResponse response = voucherService.createVoucherDetails(id, request);
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

    @GetMapping("/vouchers/customers")
    public ResponseEntity<List<CustomerResponse>> getCustomer() {
        return ResponseEntity.ok(voucherService.getAllCustomers());
    }


    @GetMapping("/vouchers/customer-group")
    public ResponseEntity<List<CustomerGroupResponse>> getCustomerGroup(){
        return ResponseEntity.ok(voucherService.getAllCustomerGroups());
    }

    @GetMapping("/vouchers/customer-group-member")
    public ResponseEntity<List<CustomerGroupMemberResponse>> getCustomerGroupMember(){
        return ResponseEntity.ok(voucherService.getAllCustomerGroupMembers());
    }

    @GetMapping("/vouchers/voucher-templates")
    public ResponseEntity<List<VoucherTemplateResponse>> getVoucherTemplates(){
        return ResponseEntity.ok(voucherTemplateService.getAll());
    }

    @PostMapping("/vouchers/voucher-templates")
    public ResponseEntity<VoucherTemplateResponse> createVoucherTemplate(@Valid @RequestBody VoucherTemplateRequest request){
        VoucherTemplateResponse response = voucherTemplateService.createVoucherTemplate(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/vouchers/voucher-templates/{id}")
    public ResponseEntity<VoucherTemplateResponse> updateVoucherTemplate(@PathVariable Long id, @Valid @RequestBody VoucherTemplateRequest request){
        VoucherTemplateResponse response = voucherTemplateService.updateVoucherTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/vouchers/voucher-templates/{id}")
    public ResponseEntity<Void> deleteVoucherTemplate(@PathVariable Long id){
        voucherTemplateService.deleteVoucherTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vouchers/voucher-templates/{id}/active")
    public ResponseEntity<String> activeVoucherTemplate(@PathVariable Long id) {
        voucherTemplateService.activeVoucherTemplate(id);
        return ResponseEntity.ok("Kích hoạt nhiệm vụ voucher");
    }

    @PostMapping("/vouchers/voucher-templates/{id}/end")
    public ResponseEntity<String> endVoucherTemplate(@PathVariable Long id) {
        voucherTemplateService.endVoucherTemplate(id);
        return ResponseEntity.ok("Kết thúc nhiệm vụ voucher");
    }

    @PostMapping("/vouchers/voucher-templates/{id}/clone")
    public ResponseEntity<VoucherTemplateResponse> cloneVoucherTemplate(@PathVariable Long id) {
        VoucherTemplateResponse response = voucherTemplateService.cloneVoucherTemplate(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/vouchers/voucher-templates/available")
    public ResponseEntity<List<VoucherTemplateResponse>> getAvailableVoucherTemplates() {
        return ResponseEntity.ok(voucherTemplateService.getAvailableVoucherTemplatesForCustomer());
    }

    @PostMapping("/vouchers/voucher-templates/{id}/redeem")
    public ResponseEntity<VoucherDetailResponse> redeemVoucherTemplate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voucherTemplateService.redeemVoucherTemplate(id));
    }
}
