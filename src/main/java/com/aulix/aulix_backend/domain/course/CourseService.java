package com.aulix.aulix_backend.domain.course;


import com.aulix.aulix_backend.domain.course.dto.*;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.shared.exception.AulixException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    //  List published courses
    @Transactional(readOnly = true)
    public List<CourseResponse> getPublishedCourses() {
        return courseRepository.findPublishedWithInstructor()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    //  Get course by slug
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> AulixException.notFound("Curso no encontrado: " + slug));
        return toFullResponse(course);
    }

    //  Create course

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        User instructor = getCurrentUser();

        String slug = generateSlug(request.getTitle());

        Course course = Course.builder()
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .thumbnailUrl(request.getThumbnailUrl())
                .instructor(instructor)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Curso creado: {} por {}", saved.getSlug(), instructor.getEmail());
        return toResponse(saved);
    }

    //  Publish / unpublish

    @Transactional
    public CourseResponse togglePublish(UUID courseId) {
        Course course = findCourseAndCheckOwnership(courseId);
        course.setPublished(!course.isPublished());
        return toResponse(courseRepository.save(course));
    }

    //  Add module

    @Transactional
    public ModuleResponse addModule(UUID courseId, String title) {
        Course course = findCourseAndCheckOwnership(courseId);

        int nextOrder = course.getModules().size();

        Module module = Module.builder()
                .course(course)
                .title(title)
                .sortOrder(nextOrder)
                .build();

        course.getModules().add(module);
        courseRepository.save(course);

        return toModuleResponse(module);
    }

    //  Add lesson

    @Transactional
    public LessonResponse addLesson(UUID moduleId, CreateLessonRequest request) {
        Module module = findModule(moduleId);
        findCourseAndCheckOwnership(module.getCourse().getId());

        int nextOrder = module.getLessons().size();

        Lesson lesson = Lesson.builder()
                .module(module)
                .title(request.getTitle())
                .type(request.getType() != null ? request.getType() : LessonType.VIDEO)
                .videoUrl(request.getVideoUrl())
                .contentMd(request.getContentMd())
                .durationSecs(request.getDurationSecs())
                .sortOrder(nextOrder)
                .build();

        module.getLessons().add(lesson);
        courseRepository.save(module.getCourse());

        return toLessonResponse(lesson);
    }

    //  Internal

    private Course findCourseAndCheckOwnership(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> AulixException.notFound("Curso no encontrado"));

        User current = getCurrentUser();
        if (!course.getInstructor().getId().equals(current.getId())) {
            throw AulixException.forbidden("No tienes permiso para modificar este curso");
        }
        return course;
    }

    private Module findModule(UUID moduleId) {
        return courseRepository.findAll().stream()
                .flatMap(c -> c.getModules().stream())
                .filter(m -> m.getId().equals(moduleId))
                .findFirst()
                .orElseThrow(() -> AulixException.notFound("Módulo no encontrado"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AulixException.notFound("Usuario no encontrado"));
    }

    private String generateSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        String slug = base;
        int counter = 1;
        while (courseRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        return slug;
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .price(course.getPrice())
                .currency(course.getCurrency())
                .thumbnailUrl(course.getThumbnailUrl())
                .published(course.isPublished())
                .instructorName(course.getInstructor().getFullName())
                .createdAt(course.getCreatedAt())
                .build();
    }

    private CourseResponse toFullResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .price(course.getPrice())
                .currency(course.getCurrency())
                .thumbnailUrl(course.getThumbnailUrl())
                .published(course.isPublished())
                .instructorName(course.getInstructor().getFullName())
                .modules(course.getModules().stream().map(this::toModuleResponse).toList())
                .createdAt(course.getCreatedAt())
                .build();
    }

    private ModuleResponse toModuleResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .sortOrder(module.getSortOrder())
                .lessons(module.getLessons().stream().map(this::toLessonResponse).toList())
                .build();
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .videoUrl(lesson.getVideoUrl())
                .durationSecs(lesson.getDurationSecs())
                .sortOrder(lesson.getSortOrder())
                .build();
    }
}

