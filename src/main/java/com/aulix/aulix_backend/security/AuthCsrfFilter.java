package com.aulix.aulix_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class AuthCsrfFilter extends OncePerRequestFilter {
    public static final String CSRF_COOKIE = "csrfToken";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> CSRF_EXEMPT_PATHS = Set.of(
            "/auth/login",
            "/auth/register"
    );
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${app.auth.csrf.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldValidate(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = cookieValue(request, CSRF_COOKIE);
        String headerToken = request.getHeader(CSRF_HEADER);

        if (!tokensMatch(cookieToken, headerToken)) {
            writeForbiddenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldValidate(HttpServletRequest request) {
        return enabled
                && MUTATING_METHODS.contains(request.getMethod().toUpperCase())
                && !CSRF_EXEMPT_PATHS.contains(request.getServletPath())
                && (cookieValue(request, ACCESS_TOKEN_COOKIE) != null
                || cookieValue(request, REFRESH_TOKEN_COOKIE) != null);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private boolean tokensMatch(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null || cookieToken.isBlank() || headerToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.getBytes(StandardCharsets.UTF_8));
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"success":false,"message":"Token CSRF inválido","timeStamp":"%s"}
                """.formatted(LocalDateTime.now()));
    }
}
