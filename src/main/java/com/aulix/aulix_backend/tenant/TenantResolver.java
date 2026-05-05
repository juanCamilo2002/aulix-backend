package com.aulix.aulix_backend.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${app.dev.default-tenant:public}")
    private String devDefaultTenant;

    public String resolveTenant(HttpServletRequest request) {
        if (activeProfile.contains("dev")) {
            String headerTenant = request.getHeader("X-Tenant-ID");
            if (headerTenant != null && !headerTenant.isBlank()) {
                return sanitize(headerTenant);
            }
            return sanitize(devDefaultTenant);
        }

        return resolveSlug(request.getServerName());
    }

    private String resolveSlug(String host) {
        // acme.aulix.com -> "acme"
        // localhost / 127.x -> "public"
        if (host == null || host.isBlank()) return "public";
        if (host.equals("localhost") || host.startsWith("127.")) return "public";

        String[] parts = host.split("\\.");
        if (parts.length >= 3) {
            return sanitize(parts[0]);
        }

        return "public";
    }

    private String sanitize(String slug) {
        return slug.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}
