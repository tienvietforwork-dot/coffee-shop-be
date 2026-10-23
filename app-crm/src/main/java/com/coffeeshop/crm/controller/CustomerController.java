package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.request.CustomerAddressRequest;
import com.coffeeshop.crm.dto.request.CustomerRequest;
import com.coffeeshop.crm.dto.response.Customer360Response;
import com.coffeeshop.crm.dto.response.CustomerAddressResponse;
import com.coffeeshop.crm.dto.response.CustomerMergeResponse;
import com.coffeeshop.crm.dto.response.CustomerResponse;
import com.coffeeshop.crm.service.Customer360Service;
import com.coffeeshop.crm.service.CustomerAddressService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Khách hàng và hồ sơ 360", description = "Quản lý hồ sơ, địa chỉ, lịch sử và hành vi khách hàng")
public class CustomerController {
    private final Customer360Service customer360Service;
    private final CustomerAddressService addressService;

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang khách hàng")
    public ResponseEntity<Page<CustomerResponse>> search(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page phải >= 0, size phải từ 1 đến 100");
        Page<com.coffeeshop.crm.entity.Customer> customers = customer360Service.searchCustomers(q, PageRequest.of(page, size));
        Map<Long, List<String>> groupsByCustomer = customer360Service.customerGroupNames(
                customers.getContent().stream().map(com.coffeeshop.crm.entity.Customer::getId).toList());
        return ResponseEntity.ok(customers.map(customer -> CustomerResponse.from(
                customer, groupsByCustomer.getOrDefault(customer.getId(), List.of()))));
    }

    @PostMapping
    @Operation(summary = "Tạo hồ sơ khách hàng")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer360Service.createCustomer(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ khách hàng")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(CustomerResponse.from(customer360Service.updateCustomer(id, request)));
    }

    @GetMapping("/{id}/360")
    @Operation(summary = "Xem hồ sơ khách hàng 360")
    public ResponseEntity<Customer360Response> get360(@PathVariable Long id,
                                    @RequestParam(required = false) LocalDate fromDate,
                                    @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(customer360Service.getCustomer360(id, fromDate, toDate));
    }

    @PostMapping("/{id}/merge")
    @Operation(summary = "Gộp hồ sơ khách hàng trùng")
    public ResponseEntity<CustomerMergeResponse> merge(@PathVariable Long id, @RequestParam Long duplicateId) {
        customer360Service.mergeCustomers(id, duplicateId);
        return ResponseEntity.ok(new CustomerMergeResponse("Đã gộp hồ sơ " + duplicateId + " vào " + id));
    }

    @GetMapping("/{customerId}/addresses")
    @Operation(summary = "Danh sách địa chỉ khách hàng")
    public ResponseEntity<List<CustomerAddressResponse>> addresses(@PathVariable Long customerId) {
        return ResponseEntity.ok(addressService.getAddresses(customerId).stream()
                .map(CustomerAddressResponse::from).toList());
    }

    @PostMapping("/{customerId}/addresses")
    @Operation(summary = "Thêm địa chỉ khách hàng")
    public ResponseEntity<CustomerAddressResponse> createAddress(@PathVariable Long customerId, @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerAddressResponse.from(addressService.create(customerId, request)));
    }

    @PutMapping("/{customerId}/addresses/{addressId}")
    @Operation(summary = "Cập nhật địa chỉ khách hàng")
    public ResponseEntity<CustomerAddressResponse> updateAddress(@PathVariable Long customerId, @PathVariable Long addressId,
                                           @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(CustomerAddressResponse.from(addressService.update(customerId, addressId, request)));
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    @Operation(summary = "Xóa địa chỉ khách hàng")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long customerId, @PathVariable Long addressId) {
        addressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
