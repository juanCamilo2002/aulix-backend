package com.aulix.aulix_backend.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCourseRequest {
    @NotBlank(message = "El titulo es requerido")
    private String title;

    private String description;

    private BigDecimal price;

    private String currency;

    private String thumbnailUrl;
}

