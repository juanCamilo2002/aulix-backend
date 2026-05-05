package com.aulix.aulix_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh"
    );

    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    @Value("${app.auth.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.auth.rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${app.auth.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupExpiredWindows();

        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000;
        String key = clientIp(request) + ":" + request.getServletPath();

        RateLimitWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.resetAt <= now) {
                return new RateLimitWindow(now + windowMillis, 1);
            }

            current.requests++;
            return current;
        });

        if (window.requests > maxRequests) {
            writeRateLimitResponse(response, window.resetAt, now);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldLimit(HttpServletRequest request) {
        return enabled
                && "POST".equalsIgnoreCase(request.getMethod())
                && LIMITED_PATHS.contains(request.getServletPath());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void cleanupExpiredWindows() {
        if (cleanupCounter.incrementAndGet() % 100 != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> entry.getValue().resetAt <= now);
    }

    private void writeRateLimitResponse(HttpServletResponse response, long resetAt, long now) throws IOException {
        long retryAfterSeconds = Math.max(1, (resetAt - now + 999) / 1000);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"success":false,"message":"Demasiados intentos. Intenta nuevamente más tarde.","timeStamp":"%s"}
                """.formatted(LocalDateTime.now()));
    }

    private static class RateLimitWindow {
        private final long resetAt;
        private int requests;

        private RateLimitWindow(long resetAt, int requests) {
            this.resetAt = resetAt;
            this.requests = requests;
        }
    }
}
