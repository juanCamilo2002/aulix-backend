package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.security.JwtService;
import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
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

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
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
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, tenant);
    }

    private AuthResponse buildAuthResponse(User user, String tenant) {
        String accessToken = jwtService.generateToken(user, tenant);
        String refreshToken = jwtService.generateRefreshToken(user, tenant);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .tenantSlug(tenant)
                .build();
    }
}
