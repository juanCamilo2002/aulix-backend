package com.aulix.aulix_backend.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantResolver tenantResolver;
    private final TenantValidator tenantValidator;

    @Override
    public boolean preHandle(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler) {
        TenantContext.clear();

        String slug = tenantResolver.resolveTenant(request);
        tenantValidator.requireActiveTenant(slug);

        TenantContext.setTenant(slug);
        log.debug("Tenant resuelto: {} para host: {}", slug, request.getServerName());

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

}

