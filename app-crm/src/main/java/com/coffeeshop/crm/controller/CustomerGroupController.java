package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.request.CustomerGroupRequest;
import com.coffeeshop.crm.dto.request.CustomerClassificationConfigRequest;
import com.coffeeshop.crm.dto.response.CustomerGroupMemberResponse;
import com.coffeeshop.crm.dto.response.CustomerGroupResponse;
import com.coffeeshop.crm.dto.response.CustomerResponse;
import com.coffeeshop.crm.dto.response.GroupEvaluationResponse;
import com.coffeeshop.crm.entity.Customer;
import com.coffeeshop.crm.entity.CustomerGroupMember;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.service.CustomerGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customer-groups")
@RequiredArgsConstructor
@Tag(name = "Phân nhóm khách hàng", description = "Tạo nhóm thủ công/tự động, xem trước, tính lại và quản lý thành viên")
public class CustomerGroupController {
    private final CustomerGroupService customerGroupService;
    private final CustomerRepository customerRepository;

    @GetMapping("/classification-config")
    public ResponseEntity<CustomerGroupService.ClassificationConfig> classificationConfig() {
        return ResponseEntity.ok(customerGroupService.classificationConfig());
    }

    @PutMapping("/classification-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerGroupService.ClassificationConfig> saveClassificationConfig(
            @Valid @RequestBody CustomerClassificationConfigRequest request) {
        return ResponseEntity.ok(customerGroupService.saveClassificationConfig(request));
    }

    @GetMapping
    @Operation(summary = "Danh sách nhóm khách hàng")
    public ResponseEntity<List<CustomerGroupResponse>> getGroups() {
        return ResponseEntity.ok(customerGroupService.getGroups().stream()
                .map(CustomerGroupResponse::from).toList());
    }

    @PostMapping
    @Operation(summary = "Tạo nhóm khách hàng")
    public ResponseEntity<CustomerGroupResponse> createGroup(@Valid @RequestBody CustomerGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerGroupResponse.from(customerGroupService.createGroup(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết nhóm khách hàng")
    public ResponseEntity<CustomerGroupResponse> getGroup(@PathVariable Long id) {
        return ResponseEntity.ok(CustomerGroupResponse.from(customerGroupService.getGroup(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật nhóm khách hàng")
    public ResponseEntity<CustomerGroupResponse> updateGroup(@PathVariable Long id, @Valid @RequestBody CustomerGroupRequest request) {
        return ResponseEntity.ok(CustomerGroupResponse.from(customerGroupService.updateGroup(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa nhóm khách hàng")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        customerGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Danh sách thành viên nhóm")
    public ResponseEntity<List<CustomerGroupMemberResponse>> getMembers(@PathVariable Long id) {
        var members = customerGroupService.getMembers(id);
        var customersById = customerRepository.findAllById(members.stream()
                        .map(CustomerGroupMember::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, customer -> customer));
        return ResponseEntity.ok(members.stream()
                .map(member -> CustomerGroupMemberResponse.from(member, customersById.get(member.getCustomerId()))).toList());
    }

    @GetMapping(value = "/{id}/members/export", produces = "text/csv")
    @Operation(summary = "Xuất danh sách thành viên nhóm ra CSV")
    public ResponseEntity<byte[]> exportMembers(@PathVariable Long id) {
        var group = customerGroupService.getGroup(id);
        var members = customerGroupService.getMembers(id);
        var customersById = customerRepository.findAllById(members.stream()
                        .map(CustomerGroupMember::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, customer -> customer));
        StringBuilder csv = new StringBuilder("\uFEFFcustomer_id,full_name,phone,email,group_name,added_at\r\n");
        for (var member : members) {
            Customer customer = customersById.get(member.getCustomerId());
            if (customer == null) continue;
            csv.append(customer.getId()).append(',').append(csv(customer.getFullName())).append(',')
                    .append(csv(customer.getPhone())).append(',').append(csv(customer.getEmail())).append(',')
                    .append(csv(group.getName())).append(',').append(member.getAddedAt() == null ? "" : member.getAddedAt())
                    .append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer-group-" + id + ".csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Xem trước khách hàng khớp tiêu chí")
    public ResponseEntity<List<CustomerResponse>> preview(@PathVariable Long id) {
        return ResponseEntity.ok(customerGroupService.preview(id).stream()
                .map(CustomerResponse::from).toList());
    }

    @PostMapping("/{id}/evaluate")
    @Operation(summary = "Tính toán lại thành viên nhóm tự động")
    public ResponseEntity<GroupEvaluationResponse> evaluate(@PathVariable Long id) {
        int count = customerGroupService.evaluateAutomaticGroup(id);
        return ResponseEntity.ok(new GroupEvaluationResponse(id, count));
    }

    @PostMapping("/{id}/members/{customerId}")
    @Operation(summary = "Thêm khách hàng vào nhóm")
    public ResponseEntity<CustomerGroupMemberResponse> addMember(@PathVariable Long id, @PathVariable Long customerId) {
        var member = customerGroupService.addMember(id, customerId);
        var customer = customerRepository.findById(customerId).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerGroupMemberResponse.from(member, customer));
    }

    @DeleteMapping("/{id}/members/{customerId}")
    @Operation(summary = "Loại khách hàng khỏi nhóm")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long customerId) {
        customerGroupService.removeMember(id, customerId);
        return ResponseEntity.noContent().build();
    }
}
