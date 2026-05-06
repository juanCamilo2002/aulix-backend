package com.aulix.aulix_backend.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantInterceptorTest {

    private TenantResolver tenantResolver;
    private TenantValidator tenantValidator;
    private TenantInterceptor tenantInterceptor;

    @BeforeEach
    void setUp() {
        tenantResolver = mock(TenantResolver.class);
        tenantValidator = mock(TenantValidator.class);
        tenantInterceptor = new TenantInterceptor(tenantResolver, tenantValidator);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void validatesActiveTenantBeforeSettingContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/courses");
        request.setServerName("acme.aulix.com");

        when(tenantResolver.resolveTenant(request)).thenReturn("acme");

        boolean result = tenantInterceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(TenantContext.getTenant()).isEqualTo("acme");
        verify(tenantValidator).requireActiveTenant("acme");
    }
}
