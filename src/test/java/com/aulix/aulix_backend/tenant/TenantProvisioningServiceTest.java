package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.model.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private TenantRepository tenantRepository;

    private TenantProvisioningService tenantProvisioningService;

    @BeforeEach
    void setUp() {
        tenantProvisioningService = new TenantProvisioningService(dataSource, tenantRepository);
        ReflectionTestUtils.setField(tenantProvisioningService, "platformAdminTenant", "acme");
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createTenantRejectsRequestsOutsidePlatformTenant() {
        TenantContext.setTenant("other");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant("new_tenant", "New Tenant", null))
                .isInstanceOf(AulixException.class)
                .hasMessage("Solo el tenant plataforma puede crear tenants");

        verifyNoInteractions(tenantRepository, dataSource);
    }

    @Test
    void createTenantRejectsInvalidSlugWithoutSanitizing() {
        TenantContext.setTenant("acme");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant("bad-tenant!", "Bad Tenant", null))
                .isInstanceOf(AulixException.class)
                .hasMessage("Slug inválido");

        verifyNoInteractions(tenantRepository, dataSource);
    }

    @Test
    void createTenantRejectsDuplicateTenantBeforeCreatingSchema() {
        TenantContext.setTenant("acme");
        when(tenantRepository.existsBySlug("new_tenant")).thenReturn(true);

        assertThatThrownBy(() -> tenantProvisioningService.createTenant("New_Tenant", "New Tenant", null))
                .isInstanceOf(AulixException.class)
                .hasMessage("Ya existe un tenant con el slug: new_tenant");

        verify(tenantRepository).existsBySlug("new_tenant");
        verifyNoInteractions(dataSource);
    }
}
