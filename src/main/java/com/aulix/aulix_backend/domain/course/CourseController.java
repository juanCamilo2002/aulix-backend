package com.aulix.aulix_backend.domain.course;

import com.aulix.aulix_backend.domain.course.dto.*;
import com.aulix.aulix_backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // Public - Anyone can view published courses
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicCourseResponse>>> getPublished() {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getPublishedCourses()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PublicCourseResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourseBySlug(slug)));
    }

    @GetMapping("/{slug}/content")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> getContentBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourseContentBySlug(slug)));
    }

    // Instructors and admins only
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CreateCourseRequest request){
        return ResponseEntity.ok(ApiResponse.ok("Curso creado", courseService.createCourse(request)));
    }

    @PatchMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> togglePublish(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.togglePublish(courseId)));
    }

    @PostMapping("/{courseId}/modules")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> addModule(
            @PathVariable UUID courseId,
            @RequestBody String title) {
        return ResponseEntity.ok(ApiResponse.ok("Módulo agregado", courseService.addModule(courseId, title)));
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> addLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateLessonRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Lección agregada", courseService.addLesson(moduleId, request)));
    }
}

