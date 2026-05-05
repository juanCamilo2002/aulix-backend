package com.aulix.aulix_backend.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "El refresh token es requerido")
    private String refreshToken;
}
