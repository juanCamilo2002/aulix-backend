package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.model.Tenant;
import com.aulix.aulix_backend.tenant.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TenantValidator {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9_]{1,63}$");

    private final TenantRepository tenantRepository;

    public void requireActiveTenant(String slug) {
        if (slug == null || !SLUG_PATTERN.matcher(slug).matches()) {
            throw AulixException.badRequest("Tenant inválido");
        }

        tenantRepository.findBySlug(slug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> AulixException.notFound("Tenant no encontrado o inactivo"));
    }
}
