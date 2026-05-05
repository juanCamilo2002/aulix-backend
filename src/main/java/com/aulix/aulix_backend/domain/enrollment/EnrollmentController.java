package com.aulix.aulix_backend.domain.enrollment;

import com.aulix.aulix_backend.domain.enrollment.dto.*;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Matricularse en un curso
    @PostMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Matriculado exitosamente",
                enrollmentService.enroll(courseId)));
    }

    // Mis cursos matriculados
    @GetMapping("/my-courses")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyCourses() {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getMyEnrollments()));
    }

    // Actualizar progreso de una lección
    @PutMapping("/courses/{courseId}/lessons/{lessonId}/progress")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody ProgressUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                enrollmentService.updateProgress(courseId, lessonId, request)));
    }

    // Ver progreso de un curso
    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getProgress(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.ok(
                enrollmentService.getCourseProgress(courseId)));
    }
}
