package com.aulix.aulix_backend.security;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 120_000L);

        user = User.builder()
                .email("student@example.com")
                .password("hashed")
                .fullName("Student")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void generateTokenCreatesAccessTokenOnly() {
        String token = jwtService.generateToken(user, "acme");

        assertThat(jwtService.extractUsername(token)).isEqualTo("student@example.com");
        assertThat(jwtService.extractTenantSlug(token)).isEqualTo("acme");
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.isAccessTokenValid(token, user)).isTrue();
    }

    @Test
    void generateRefreshTokenCreatesRefreshTokenOnly() {
        String token = jwtService.generateRefreshToken(user, "acme", "refresh-id");

        assertThat(jwtService.extractTenantSlug(token)).isEqualTo("acme");
        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.isRefreshTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidWhenAccountIsDisabled() {
        String token = jwtService.generateToken(user, "acme");
        user.setEnabled(false);

        assertThat(jwtService.isAccessTokenValid(token, user)).isFalse();
    }
}
