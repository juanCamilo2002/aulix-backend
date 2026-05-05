package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthService authService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "accessTokenExpirationMs", 60_000L);
        ReflectionTestUtils.setField(controller, "refreshTokenExpirationMs", 120_000L);
        ReflectionTestUtils.setField(controller, "secureCookies", false);
        ReflectionTestUtils.setField(controller, "sameSite", "Lax");
    }

    @Test
    void loginSetsHttpOnlyAuthCookiesAndHidesTokensFromBody() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("password");

        when(authService.login(request)).thenReturn(AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("student@example.com")
                .fullName("Student")
                .role("STUDENT")
                .tenantSlug("acme")
                .build());

        ResponseEntity<ApiResponse<AuthResponse>> response = controller.login(request);

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(3);
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("accessToken=access-token")
                && cookie.contains("HttpOnly"));
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("refreshToken=refresh-token")
                && cookie.contains("HttpOnly"));
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("csrfToken=")
                && !cookie.contains("HttpOnly"));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isNull();
        assertThat(response.getBody().getData().getRefreshToken()).isNull();
    }
}
