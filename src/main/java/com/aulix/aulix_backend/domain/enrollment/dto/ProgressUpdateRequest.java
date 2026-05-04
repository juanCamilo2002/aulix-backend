package com.aulix.aulix_backend.domain.enrollment.dto;

import lombok.Data;

@Data
public class ProgressUpdateRequest {
    private boolean completed;
    private int lastPosition;
}

