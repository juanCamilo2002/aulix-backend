package com.aulix.aulix_backend.domain.course.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicModuleResponse {
    private UUID id;
    private String title;
    private int sortOrder;
    private List<PublicLessonResponse> lessons;
}
