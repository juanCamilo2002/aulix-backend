package com.aulix.aulix_backend.domain.course.dto;

import com.aulix.aulix_backend.domain.course.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLessonRequest {

    @NotBlank(message = "El título es requerido")
    @Size(max = 255, message = "El título no puede superar 255 caracteres")
    private String title;

    private LessonType type;

    @Size(max = 2048, message = "La URL del video no puede superar 2048 caracteres")
    @Pattern(regexp = "^https?://.+$", message = "La URL del video debe iniciar con http:// o https://")
    private String videoUrl;

    @Size(max = 20000, message = "El contenido no puede superar 20000 caracteres")
    private String contentMd;

    @PositiveOrZero(message = "La duración no puede ser negativa")
    private int durationSecs;
}

