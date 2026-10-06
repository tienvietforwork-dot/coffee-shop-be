package com.coffeeshop.controller;

import com.coffeeshop.dto.request.RolePermissionsRequest;
import com.coffeeshop.dto.request.UserRequest;
import com.coffeeshop.dto.response.PermissionResponse;
import com.coffeeshop.dto.response.RoleResponse;
import com.coffeeshop.dto.response.UserResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.RoleService;
import com.coffeeshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tài khoản & phân quyền (user – role – permission). */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping("/api/users")
    @PreAuthorize(Perm.CAN_USERS)
    public List<UserResponse> users() {
        return userService.findAll();
    }

    @PostMapping("/api/users")
    @PreAuthorize(Perm.CAN_USERS)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize(Perm.CAN_USERS)
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize(Perm.CAN_USERS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/api/roles")
    @PreAuthorize(Perm.CAN_READ_ROLES)
    public List<RoleResponse> roles() {
        return roleService.roles();
    }

    @GetMapping("/api/permissions")
    @PreAuthorize(Perm.CAN_READ_ROLES)
    public List<PermissionResponse> permissions() {
        return roleService.permissions();
    }

    @PutMapping("/api/roles/{id}/permissions")
    @PreAuthorize(Perm.CAN_ROLES)
    public RoleResponse setPermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionsRequest request) {
        return roleService.setPermissions(id, request.permissionCodes());
    }
}
