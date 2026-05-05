package com.aulix.aulix_backend.domain.enrollment.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProgressUpdateRequest {
    private boolean completed;

    @Min(0)
    private int lastPosition;
}

