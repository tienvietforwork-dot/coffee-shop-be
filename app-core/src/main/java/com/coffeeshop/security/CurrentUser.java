package com.coffeeshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

/** Who is acting right now – used for audit columns and to record the employee / customer on business rows. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p : null;
    }

    /** Username for created_by / updated_by / del_user: the login, "guest" for anonymous web requests, "system" for jobs. */
    public static String username() {
        UserPrincipal p = principal();
        if (p != null) return p.getUsername();
        return RequestContextHolder.getRequestAttributes() != null ? "guest" : "system";
    }

    public static Long staffId() {
        UserPrincipal p = principal();
        return p == null ? null : p.getStaffId();
    }

    public static Long customerId() {
        UserPrincipal p = principal();
        return p == null ? null : p.getCustomerId();
    }

    public static Long userId() {
        UserPrincipal p = principal();
        return p == null ? null : p.getId();
    }
}
