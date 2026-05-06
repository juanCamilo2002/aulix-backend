package com.aulix.aulix_backend.domain.course.dto;

import com.aulix.aulix_backend.domain.course.LessonType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PublicLessonResponse {
    private UUID id;
    private String title;
    private LessonType type;
    private int durationSecs;
    private int sortOrder;
}
