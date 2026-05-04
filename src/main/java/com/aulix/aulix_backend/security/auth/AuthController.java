package com.aulix.aulix_backend.security.auth;

import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.security.auth.dto.AuthResponse;
import com.aulix.aulix_backend.security.auth.dto.LoginRequest;
import com.aulix.aulix_backend.security.auth.dto.RegisterRequest;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

