package com.aulix.aulix_backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantRequest {
    @NotBlank(message = "El slug es requerido")
    private String slug;

    @NotBlank(message = "El nombre es requerido")
    private String name;

    private String brandColor;
}
