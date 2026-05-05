package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.CurrentUserResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.security.auth.dto.RefreshTokenRequest;
import com.aulix.aulix_backend.security.auth.dto.RegisterRequest;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registro exitoso", response));
    }

    @PostMapping("/register/instructor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> registerInstructor(
            @Valid @RequestBody RegisterRequest request){
        AuthResponse response = authService.registerWithRole(request, Role.INSTRUCTOR);
        return ResponseEntity.ok(ApiResponse.ok("Instructor registrado", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login existoso", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Sesión renovada", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada", null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(user)));
    }
}

