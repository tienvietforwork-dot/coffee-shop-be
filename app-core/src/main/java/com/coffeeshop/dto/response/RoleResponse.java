package com.coffeeshop.dto.response;

import java.util.List;

public record RoleResponse(Long id, String code, String name, String description, boolean active,
                           List<String> permissionCodes, long userCount) {
}
