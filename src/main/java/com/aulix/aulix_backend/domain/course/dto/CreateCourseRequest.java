package com.aulix.aulix_backend.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCourseRequest {
    @NotBlank(message = "El título es requerido")
    @Size(max = 255, message = "El título no puede superar 255 caracteres")
    private String title;

    @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
    private String description;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio debe tener máximo 8 enteros y 2 decimales")
    private BigDecimal price;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe usar código ISO de 3 letras en mayúscula")
    private String currency;

    @Size(max = 2048, message = "La URL de miniatura no puede superar 2048 caracteres")
    @Pattern(regexp = "^https?://.+$", message = "La URL de miniatura debe iniciar con http:// o https://")
    private String thumbnailUrl;
}

