package com.aulix.aulix_backend.domain.enrollment.dto;

import com.aulix.aulix_backend.domain.enrollment.EnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EnrollmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String courseSlug;
    private String courseThumbnail;
    private EnrollmentStatus status;
    private BigDecimal amountPaid;
    private int totalLessons;
    private int completedLessons;
    private int progressPercent;
    private LocalDateTime enrolledAt;
}

