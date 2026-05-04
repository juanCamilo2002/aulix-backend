package com.aulix.aulix_backend.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${app.dev.default-tenant:public}")
    private String devDefaultTenant;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (activeProfile.contains("dev")) {
            // Header tiene prioridad (para probar otros tenants)
            String headerTenant = request.getHeader("X-Tenant-ID");
            if (headerTenant != null && !headerTenant.isBlank()) {
                TenantContext.setTenant(sanitize(headerTenant));
                return true;
            }
            // Si no hay header, usar el tenant por defecto de dev
            TenantContext.setTenant(devDefaultTenant);
            return true;
        }
        String host = request.getServerName();
        String slug = resolveSlug(host);

        TenantContext.setTenant(slug);
        log.debug("Tenant resuelto: {} para host: {}", slug, host);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // CRITICAL: Always clean up to avoid memory leaks in the thread group
        TenantContext.clear();
    }

    private String resolveSlug(String host) {
        // acme.aulix.com   â†’ "acme"
        // localhost         â†’ "public" (desarrollo)
        // 127.0.0.1         â†’ "public" (desarrollo)
        if (host == null || host.isBlank()) return "public";
        if (host.equals("localhost") || host.startsWith("127.")) return "public";

        String[] parts = host.split("\\.");
        if (parts.length >= 3) {
            return sanitize(parts[0]);
        }

        return "public";
    }

    private String sanitize(String slug) {
        // Only lowercase letters, numbers, and underscores
        // CRITICAL: Prevents SQL injection in SET search_path
        return slug.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

}

