package com.aulix.aulix_backend.domain.enrollment.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ProgressUpdateRequest {
    private boolean completed;

    @PositiveOrZero(message = "La posición no puede ser negativa")
    private int lastPosition;
}

