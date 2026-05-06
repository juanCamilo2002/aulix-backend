package com.aulix.aulix_backend.domain.course.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicCourseResponse {
    private UUID id;
    private String title;
    private String slug;
    private String description;
    private BigDecimal price;
    private String currency;
    private String thumbnailUrl;
    private boolean published;
    private String instructorName;
    private List<PublicModuleResponse> modules;
    private LocalDateTime createdAt;
}
