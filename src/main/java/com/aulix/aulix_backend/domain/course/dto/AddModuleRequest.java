package com.aulix.aulix_backend.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddModuleRequest {
    @NotBlank(message = "El título es requerido")
    @Size(max = 255, message = "El título no puede superar 255 caracteres")
    private String title;
}
