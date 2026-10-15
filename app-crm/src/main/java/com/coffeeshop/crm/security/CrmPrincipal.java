package com.coffeeshop.crm.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/** Identity and linked customer profile carried by the shared app-core JWT. */
public record CrmPrincipal(String username, Long userId) implements AuthenticatedPrincipal {
    @Override
    public String getName() {
        return username;
    }
}
