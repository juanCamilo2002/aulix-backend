package com.aulix.aulix_backend.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es requerido")
    private String fullName;

    @Email(message = "Email invÃ¡lido")
    @NotBlank(message = "El email es requerido")
    private String email;

    @NotBlank(message = "La contraseÃ±a es requerida")
    @Size(min = 8, message = "La constraseÃ±a debe tener al menos 8 caracteres")
    private String password;
}

