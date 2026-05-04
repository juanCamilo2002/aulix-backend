package com.aulix.aulix_backend.domain.course.dto;

import com.aulix.aulix_backend.domain.course.LessonType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LessonResponse {
    private UUID id;
    private String title;
    private LessonType type;
    private String videoUrl;
    private int durationSecs;
    private int sortOrder;
}

