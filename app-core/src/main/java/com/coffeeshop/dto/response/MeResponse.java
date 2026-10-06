package com.coffeeshop.dto.response;

import java.util.List;

public record MeResponse(UserResponse user, List<PermissionResponse> permissions) {
}
