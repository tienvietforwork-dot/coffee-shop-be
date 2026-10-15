package com.coffeeshop.crm.controller;

import com.coffeeshop.crm.dto.request.CustomerGroupRequest;
import com.coffeeshop.crm.dto.response.CustomerGroupMemberResponse;
import com.coffeeshop.crm.dto.response.CustomerGroupResponse;
import com.coffeeshop.crm.dto.response.CustomerResponse;
import com.coffeeshop.crm.dto.response.GroupEvaluationResponse;
import com.coffeeshop.crm.service.CustomerGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customer-groups")
@RequiredArgsConstructor
@Tag(name = "Phân nhóm khách hàng", description = "Tạo nhóm thủ công/tự động, xem trước, tính lại và quản lý thành viên")
public class CustomerGroupController {
    private final CustomerGroupService customerGroupService;

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
        return ResponseEntity.ok(customerGroupService.getMembers(id).stream()
                .map(CustomerGroupMemberResponse::from).toList());
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerGroupMemberResponse.from(customerGroupService.addMember(id, customerId)));
    }

    @DeleteMapping("/{id}/members/{customerId}")
    @Operation(summary = "Loại khách hàng khỏi nhóm")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long customerId) {
        customerGroupService.removeMember(id, customerId);
        return ResponseEntity.noContent().build();
    }
}
