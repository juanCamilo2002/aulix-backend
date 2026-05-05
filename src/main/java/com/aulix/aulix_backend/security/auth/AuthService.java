package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.security.JwtService;
import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.CurrentUserResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.security.auth.dto.RefreshTokenRequest;
import com.aulix.aulix_backend.security.auth.dto.RegisterRequest;
import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String tenant = TenantContext.getTenant();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw AulixException.conflict("Ya existe un usuario con ese email");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.STUDENT)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("Usuario registrado: {} en tenant: {}", user.getEmail(), tenant);
        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String tenant = TenantContext.getTenant();

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AulixException.unauthorized("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw AulixException.unauthorized("Credenciales inválidas");
        }

        ensureUserCanAuthenticate(user);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login exitoso: {} en tenant: {}", user.getEmail(), tenant);
        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public AuthResponse registerWithRole(RegisterRequest request, Role role) {
        String tenant = TenantContext.getTenant();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw AulixException.conflict("Ya existe un usuario con ese email");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        return refresh(request.getRefreshToken());
    }

    @Transactional
    public AuthResponse refresh(String token) {
        if (token == null || token.isBlank()) {
            throw AulixException.unauthorized("Refresh token inválido");
        }

        String tenant = TenantContext.getTenant();

        User user = getUserFromRefreshToken(token, tenant);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> AulixException.unauthorized("Refresh token inválido"));

        if (storedToken.isRevoked()) {
            revokeAllActiveTokens(user);
            throw AulixException.unauthorized("Refresh token inválido");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            revokeToken(storedToken, null);
            throw AulixException.unauthorized("Refresh token expirado");
        }

        String newTokenId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(user, tenant);
        String refreshToken = jwtService.generateRefreshToken(user, tenant, newTokenId);

        revokeToken(storedToken, newTokenId);
        saveRefreshToken(user, newTokenId, refreshToken);

        return toAuthResponse(user, tenant, accessToken, refreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        logout(request.getRefreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(refreshToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> revokeToken(token, null));
    }

    public CurrentUserResponse me(User user) {
        return CurrentUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .tenantSlug(TenantContext.getTenant())
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String tenant) {
        String accessToken = jwtService.generateToken(user, tenant);
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user, tenant, refreshTokenId);

        saveRefreshToken(user, refreshTokenId, refreshToken);

        return toAuthResponse(user, tenant, accessToken, refreshToken);
    }

    private AuthResponse toAuthResponse(User user, String tenant, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .tenantSlug(tenant)
                .build();
    }

    private User getUserFromRefreshToken(String token, String tenant) {
        try {
            String tokenTenant = jwtService.extractTenantSlug(token);
            String email = jwtService.extractUsername(token);

            if (!tenant.equals(tokenTenant)) {
                throw AulixException.unauthorized("Refresh token inválido");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> AulixException.unauthorized("Refresh token inválido"));

            ensureUserCanAuthenticate(user);

            if (!jwtService.isRefreshTokenValid(token, user)) {
                throw AulixException.unauthorized("Refresh token inválido");
            }

            return user;
        } catch (AulixException ex) {
            throw ex;
        } catch (Exception ex) {
            throw AulixException.unauthorized("Refresh token inválido");
        }
    }

    private void saveRefreshToken(User user, String tokenId, String token) {
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenId(tokenId)
                .tokenHash(hashToken(token))
                .user(user)
                .expiresAt(toLocalDateTime(jwtService.extractExpiration(token)))
                .build());
    }

    private void ensureUserCanAuthenticate(User user) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw AulixException.unauthorized("Cuenta no disponible");
        }
    }

    private void revokeToken(RefreshToken token, String replacedByTokenId) {
        token.setRevokedAt(LocalDateTime.now());
        token.setReplacedByTokenId(replacedByTokenId);
        refreshTokenRepository.save(token);
    }

    private void revokeAllActiveTokens(User user) {
        refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)
                .forEach(token -> revokeToken(token, null));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
