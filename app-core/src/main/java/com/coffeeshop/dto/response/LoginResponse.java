package com.coffeeshop.dto.response;

import java.util.List;

/** Login / sign-up result: token + account + the screens it may open (for the menu). */
public record LoginResponse(String token, UserResponse user, List<PermissionResponse> permissions) {
}
