package com.aulix.aulix_backend.domain.course.dto;

import com.aulix.aulix_backend.domain.course.LessonType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLessonRequest {

    @NotBlank(message = "El titulo es requerido")
    private String title;

    private LessonType type;
    private String videoUrl;
    private String contentMd;
    private int durationSecs;
}

