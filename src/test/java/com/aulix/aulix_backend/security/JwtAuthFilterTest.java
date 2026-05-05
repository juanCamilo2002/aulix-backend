package com.aulix.aulix_backend.security;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.security.auth.AuthService;
import com.aulix.aulix_backend.tenant.TenantResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private AuthService authService;
    private TenantResolver tenantResolver;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        authService = mock(AuthService.class);
        tenantResolver = mock(TenantResolver.class);
        filter = new JwtAuthFilter(jwtService, authService, tenantResolver);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsingAccessTokenCookie() throws Exception {
        User user = User.builder()
                .email("student@example.com")
                .password("hashed")
                .role(Role.STUDENT)
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/enrollments/my-courses");
        request.setCookies(new Cookie("accessToken", "access-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isAccessToken("access-token")).thenReturn(true);
        when(jwtService.extractUsername("access-token")).thenReturn("student@example.com");
        when(jwtService.extractTenantSlug("access-token")).thenReturn("acme");
        when(tenantResolver.resolveTenant(request)).thenReturn("acme");
        when(authService.loadUserByUsername("student@example.com")).thenReturn(user);
        when(jwtService.isAccessTokenValid("access-token", user)).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("student@example.com");
    }

    @Test
    void doesNotAuthenticateWhenTokenTenantDoesNotMatchRequestTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/enrollments/my-courses");
        request.setCookies(new Cookie("accessToken", "access-token"));

        when(jwtService.isAccessToken("access-token")).thenReturn(true);
        when(jwtService.extractUsername("access-token")).thenReturn("student@example.com");
        when(jwtService.extractTenantSlug("access-token")).thenReturn("acme");
        when(tenantResolver.resolveTenant(request)).thenReturn("other");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authService, never()).loadUserByUsername(anyString());
    }
}
