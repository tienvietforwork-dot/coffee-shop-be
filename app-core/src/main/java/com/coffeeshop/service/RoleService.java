package com.coffeeshop.service;

import com.coffeeshop.dto.response.PermissionResponse;
import com.coffeeshop.dto.response.RoleResponse;
import com.coffeeshop.entity.Permission;
import com.coffeeshop.entity.Role;
import com.coffeeshop.entity.RolePermission;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.PermissionRepository;
import com.coffeeshop.repository.RolePermissionRepository;
import com.coffeeshop.repository.RoleRepository;
import com.coffeeshop.repository.UserRoleRepository;
import com.coffeeshop.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The 3 fixed roles; only their permission sets are editable (màn "Phân quyền"). */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream()
                .sorted((a, b) -> order(a.getCode()) - order(b.getCode()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        return permissionRepository.findAllByOrderBySortOrderAscCodeAsc().stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public RoleResponse setPermissions(Long roleId, List<String> codes) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role: " + roleId));
        Set<String> wanted = new HashSet<>(codes);
        if (Role.ADMIN.equals(role.getCode()) && !(wanted.contains(Perm.ROLES) && wanted.contains(Perm.USERS))) {
            throw new BadRequestException("Role Quản lý phải giữ quyền Tài khoản và Phân quyền");
        }
        Map<String, Permission> all = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));
        for (String c : wanted) if (!all.containsKey(c)) throw new BadRequestException("Quyền không tồn tại: " + c);

        List<RolePermission> current = rolePermissionRepository.findByRoleId(roleId);
        Set<String> have = new HashSet<>();
        for (RolePermission rp : current) {
            if (wanted.contains(rp.getPermission().getCode())) have.add(rp.getPermission().getCode());
            else rp.softDelete();
        }
        for (String c : wanted) {
            if (!have.contains(c)) {
                rolePermissionRepository.save(RolePermission.builder().role(role).permission(all.get(c)).build());
            }
        }
        rolePermissionRepository.flush();
        permissionService.invalidateAll();
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isActive(),
                wanted.stream().sorted().toList(), userRoleRepository.countUsers(roleId));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isActive(),
                rolePermissionRepository.findByRoleId(role.getId()).stream().map(rp -> rp.getPermission().getCode()).sorted().toList(),
                userRoleRepository.countUsers(role.getId()));
    }

    private static int order(String code) {
        return switch (code) {
            case Role.ADMIN -> 0;
            case Role.STAFF -> 1;
            default -> 2;
        };
    }
}
