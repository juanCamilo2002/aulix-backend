package com.aulix.aulix_backend.domain.enrollment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProgressResponse {
    private UUID lessonId;
    private boolean completed;
    private int lastPosition;
}

