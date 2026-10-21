package com.coffeeshop.config;

import com.coffeeshop.entity.AuditLog;
import com.coffeeshop.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Puts every write request to /api (POST, PUT, PATCH, DELETE) on the audit queue after it has run: who, what, which
 * record, the result. Runs after Spring Security, so the logged-in user is known; on login the typed username is used.
 * Passwords and tokens in the body are masked.
 */
@Component
@RequiredArgsConstructor
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern SECRET = Pattern.compile("(\"(?:password|oldPassword|newPassword|currentPassword|token|secret|apiKey)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LOGIN_USER = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]{1,50})\"");
    private static final Pattern ID = Pattern.compile("^\\d+$|^[A-Z]{2}\\d{6}[-A-Z0-9]*$");
    private static final int MAX_DETAIL = 1000;

    private final AuditLogService auditLogService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !WRITES.contains(request.getMethod()) || !uri.startsWith("/api/")
                || uri.startsWith("/api/images") // binary uploads
                || uri.contains("/ipn") || uri.contains("/webhook"); // partner callbacks are logged by their own services
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, MAX_DETAIL * 4);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(wrapped, response);
        } finally {
            try {
                auditLogService.enqueue(entry(wrapped, response.getStatus(), System.currentTimeMillis() - start));
            } catch (RuntimeException ignored) {
                // the log must never break the request
            }
        }
    }

    private AuditLog entry(ContentCachingRequestWrapper request, int status, long ms) {
        String path = request.getRequestURI();
        String[] parts = path.substring("/api/".length()).split("/");
        String module = parts[0].equals("public") && parts.length > 1 ? "public/" + parts[1] : parts[0];
        String entityId = null;
        for (String p : parts) {
            if (ID.matcher(p).matches()) {
                entityId = p;
                break;
            }
        }
        String last = parts[parts.length - 1];
        String action = switch (request.getMethod()) {
            case "DELETE" -> "DELETE";
            case "PUT", "PATCH" -> "UPDATE";
            default -> entityId == null || last.equals(entityId) || parts.length <= 1 ? "CREATE" : last;
        };
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        String detail = body.isBlank() ? null : SECRET.matcher(body).replaceAll("$1\"***\"");
        if (detail != null && detail.length() > MAX_DETAIL) detail = detail.substring(0, MAX_DETAIL) + "…";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken) ? auth.getName() : null;
        if (username == null && path.startsWith("/api/auth/login")) {
            var m = LOGIN_USER.matcher(body);
            if (m.find()) username = m.group(1);
        }
        String ip = request.getHeader("X-Forwarded-For");
        ip = ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.split(",")[0].trim();
        return AuditLog.builder().username(username).module(module).action(action.length() > 40 ? action.substring(0, 40) : action)
                .method(request.getMethod()).path(path.length() > 255 ? path.substring(0, 255) : path).entityId(entityId)
                .statusCode(status).durationMs(ms).ip(ip.length() > 64 ? ip.substring(0, 64) : ip).detail(detail)
                .createdAt(LocalDateTime.now()).build();
    }
}
