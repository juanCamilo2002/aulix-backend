package com.aulix.aulix_backend.domain.enrollment;

import com.aulix.aulix_backend.domain.course.Course;
import com.aulix.aulix_backend.domain.course.CourseRepository;
import com.aulix.aulix_backend.domain.course.Lesson;
import com.aulix.aulix_backend.domain.course.LessonRepository;
import com.aulix.aulix_backend.domain.enrollment.dto.*;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.shared.exception.AulixException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    // â”€â”€ Enroll current user â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public EnrollmentResponse enroll(UUID courseId) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> AulixException.notFound("Curso no encontrado"));

        if (!course.isPublished()) {
            throw AulixException.badRequest("El curso no estÃ¡ disponible");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw AulixException.conflict("Ya estÃ¡s matriculado en este curso");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .amountPaid(course.getPrice())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Usuario {} matriculado en curso {}", user.getEmail(), course.getSlug());

        return toResponse(saved, 0);
    }

    // â”€â”€ Mis cursos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments() {
        User user = getCurrentUser();
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId())
                .stream()
                .map(e -> {
                    long completed = progressRepository.countCompletedByEnrollmentId(e.getId());
                    return toResponse(e, (int) completed);
                })
                .toList();
    }

    // â”€â”€ Actualizar progreso de una lecciÃ³n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public ProgressResponse updateProgress(UUID courseId, UUID lessonId,
                                           ProgressUpdateRequest request) {
        User user = getCurrentUser();

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> AulixException.forbidden("No estÃ¡s matriculado en este curso"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> AulixException.notFound("LecciÃ³n no encontrada"));

        LessonProgress progress = progressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .build());

        progress.setCompleted(request.isCompleted());
        progress.setLastPosition(request.getLastPosition());
        progressRepository.save(progress);

        // Verificar si el curso estÃ¡ completo
        checkCourseCompletion(enrollment);

        return ProgressResponse.builder()
                .lessonId(lessonId)
                .completed(progress.isCompleted())
                .lastPosition(progress.getLastPosition())
                .build();
    }

    // â”€â”€ Track course progress â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public List<ProgressResponse> getCourseProgress(UUID courseId) {
        User user = getCurrentUser();

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> AulixException.forbidden("No estÃ¡s matriculado en este curso"));

        return progressRepository.findByEnrollmentId(enrollment.getId())
                .stream()
                .map(p -> ProgressResponse.builder()
                        .lessonId(p.getLesson().getId())
                        .completed(p.isCompleted())
                        .lastPosition(p.getLastPosition())
                        .build())
                .toList();
    }

    // â”€â”€ Internal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void checkCourseCompletion(Enrollment enrollment) {
        Course course = enrollment.getCourse();

        long totalLessons = course.getModules().stream()
                .mapToLong(m -> m.getLessons().size())
                .sum();

        long completedLessons = progressRepository
                .countCompletedByEnrollmentId(enrollment.getId());

        if (totalLessons > 0 && completedLessons >= totalLessons) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollmentRepository.save(enrollment);
            log.info("Curso completado por usuario: {}", enrollment.getUser().getEmail());
        }
    }

    private int countTotalLessons(Course course) {
        return course.getModules().stream()
                .mapToInt(m -> m.getLessons().size())
                .sum();
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, int completedLessons) {
        Course course = enrollment.getCourse();
        int total = countTotalLessons(course);
        int percent = total > 0 ? (int) ((completedLessons * 100.0) / total) : 0;

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .courseThumbnail(course.getThumbnailUrl())
                .status(enrollment.getStatus())
                .amountPaid(enrollment.getAmountPaid())
                .totalLessons(total)
                .completedLessons(completedLessons)
                .progressPercent(percent)
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AulixException.notFound("Usuario no encontrado"));
    }
}

