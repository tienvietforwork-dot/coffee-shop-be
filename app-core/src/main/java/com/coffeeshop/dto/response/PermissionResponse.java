package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Permission;

public record PermissionResponse(Long id, String code, String name, String path, String module, String description,
                                 Integer sortOrder) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getPath(), p.getModule(),
                p.getDescription(), p.getSortOrder());
    }
}
