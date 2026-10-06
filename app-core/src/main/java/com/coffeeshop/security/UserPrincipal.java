package com.coffeeshop.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Authenticated account: authorities = "ROLE_" + role codes + permission codes. */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean active;
    private final List<String> roles;
    private final Set<String> permissions;
    private final Long staffId;
    private final Long customerId;

    public UserPrincipal(Long id, String username, String password, boolean active, List<String> roles,
                         Set<String> permissions, Long staffId, Long customerId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.active = active;
        this.roles = roles;
        this.permissions = permissions;
        this.staffId = staffId;
        this.customerId = customerId;
    }

    /** Highest role, kept as the JWT "role" claim that app-crm/app-promotions/app-stats check. */
    public String getPrimaryRole() {
        for (String r : List.of("ADMIN", "STAFF", "CUSTOMER")) if (roles.contains(r)) return r;
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        roles.forEach(r -> list.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> list.add(new SimpleGrantedAuthority(p)));
        return list;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
