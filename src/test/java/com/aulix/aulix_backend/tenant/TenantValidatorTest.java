package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.model.Tenant;
import com.aulix.aulix_backend.tenant.model.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantValidatorTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantValidator tenantValidator;

    @Test
    void acceptsActiveTenant() {
        Tenant tenant = Tenant.builder()
                .slug("acme")
                .active(true)
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));

        tenantValidator.requireActiveTenant("acme");

        verify(tenantRepository).findBySlug("acme");
    }

    @Test
    void rejectsInvalidSlugBeforeQueryingRepository() {
        assertThatThrownBy(() -> tenantValidator.requireActiveTenant("!!!"))
                .isInstanceOf(AulixException.class)
                .hasMessage("Tenant inválido");

        verifyNoInteractions(tenantRepository);
    }

    @Test
    void rejectsInactiveTenant() {
        Tenant tenant = Tenant.builder()
                .slug("acme")
                .active(false)
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantValidator.requireActiveTenant("acme"))
                .isInstanceOf(AulixException.class)
                .hasMessage("Tenant no encontrado o inactivo");
    }
}
