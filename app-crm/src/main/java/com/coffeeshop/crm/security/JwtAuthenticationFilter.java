package com.coffeeshop.crm.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtUtil.parseClaims(token);
                String username = claims.getSubject();
                Long userId = longClaim(claims.get("userId"));

                Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof Collection<?> roles) {
                    roles.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .filter(value -> !value.isBlank())
                            .map(value -> new SimpleGrantedAuthority("ROLE_" + value))
                            .forEach(authorities::add);
                }
                String role = claims.get("role", String.class);
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                Object permissionsClaim = claims.get("perms");
                if (permissionsClaim instanceof Collection<?> permissions) {
                    permissions.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .filter(permission -> !permission.isBlank())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }

                CrmPrincipal principal = new CrmPrincipal(username, userId);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long longClaim(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string && !string.isBlank()) return Long.parseLong(string);
        return null;
    }
}
