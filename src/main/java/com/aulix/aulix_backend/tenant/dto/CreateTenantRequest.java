package com.aulix.aulix_backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantRequest {
    @NotBlank(message = "El slug es requerido")
    @Size(max = 63, message = "El slug no puede superar 63 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El slug solo puede contener letras, números y guiones bajos")
    private String slug;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String name;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color de marca debe ser hexadecimal")
    private String brandColor;
}
