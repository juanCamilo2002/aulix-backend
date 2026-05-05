package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.CurrentUserResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.security.auth.dto.RegisterRequest;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.auth.cookie.secure:false}")
    private boolean secureCookies;

    @Value("${app.auth.cookie.same-site:Lax}")
    private String sameSite;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return authResponse("Registro exitoso", response);
    }

    @PostMapping("/register/instructor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> registerInstructor(
            @Valid @RequestBody RegisterRequest request){
        AuthResponse response = authService.registerWithRole(request, Role.INSTRUCTOR);
        stripTokens(response);
        return ResponseEntity.ok(ApiResponse.ok("Instructor registrado", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return authResponse("Login existoso", response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthResponse response = authService.refresh(refreshToken);
        return authResponse("Sesión renovada", response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .headers(clearAuthCookies())
                .body(ApiResponse.ok("Sesión cerrada", null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(user)));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> authResponse(String message, AuthResponse response) {
        HttpHeaders headers = authCookies(response.getAccessToken(), response.getRefreshToken());
        stripTokens(response);

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok(message, response));
    }

    private HttpHeaders authCookies(String accessToken, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                Duration.ofMillis(accessTokenExpirationMs)).toString());
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationMs)).toString());
        return headers;
    }

    private HttpHeaders clearAuthCookies() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_COOKIE, "", Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN_COOKIE, "", Duration.ZERO).toString());
        return headers;
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private void stripTokens(AuthResponse response) {
        response.setAccessToken(null);
        response.setRefreshToken(null);
    }
}

