package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.security.JwtService;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.security.auth.dto.RefreshTokenRequest;
import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
        TenantContext.setTenant("acme");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void loginRejectsWrongPassword() {
        LoginRequest request = loginRequest();
        User user = user();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AulixException.class)
                .hasMessage("Credenciales inválidas");

        verify(jwtService, never()).generateToken(any(), anyString());
        verify(userRepository, never()).save(user);
    }

    @Test
    void loginRejectsDisabledAccount() {
        LoginRequest request = loginRequest();
        User user = user();
        user.setEnabled(false);

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AulixException.class)
                .hasMessage("Cuenta no disponible");

        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void refreshRejectsTokenForDifferentTenant() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(jwtService.extractTenantSlug("refresh-token")).thenReturn("other");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AulixException.class)
                .hasMessage("Refresh token inválido");

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("wrong-password");
        return request;
    }

    private User user() {
        return User.builder()
                .email("student@example.com")
                .password("hashed-password")
                .fullName("Student")
                .role(Role.STUDENT)
                .build();
    }
}
