package com.aulix.aulix_backend.domain.enrollment;

import com.aulix.aulix_backend.domain.course.Course;
import com.aulix.aulix_backend.domain.course.CourseRepository;
import com.aulix.aulix_backend.domain.course.Lesson;
import com.aulix.aulix_backend.domain.course.LessonRepository;
import com.aulix.aulix_backend.domain.course.Module;
import com.aulix.aulix_backend.domain.enrollment.dto.EnrollmentResponse;
import com.aulix.aulix_backend.domain.enrollment.dto.ProgressResponse;
import com.aulix.aulix_backend.domain.enrollment.dto.ProgressUpdateRequest;
import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.shared.exception.AulixException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository progressRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User student;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .password("hashed")
                .fullName("Student")
                .role(Role.STUDENT)
                .build();

        course = Course.builder()
                .id(UUID.randomUUID())
                .title("Test Course")
                .slug("test-course")
                .published(true)
                .price(BigDecimal.ZERO)
                .instructor(User.builder().email("instructor@example.com").build())
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("student@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void enrollCreatesEnrollmentWhenNotAlreadyEnrolled() {
        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = enrollmentService.enroll(course.getId());

        assertThat(result.getCourseId()).isEqualTo(course.getId());
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enrollThrowsWhenCourseNotPublished() {
        course.setPublished(false);

        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));

        assertThatThrownBy(() -> enrollmentService.enroll(course.getId()))
                .isInstanceOf(AulixException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    void enrollThrowsWhenAlreadyEnrolled() {
        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(course.getId()))
                .isInstanceOf(AulixException.class)
                .hasMessageContaining("matriculado");
    }

    @Test
    void updateProgressValidatesLessonBelongsToCourse() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .user(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        Module module = Module.builder()
                .id(UUID.randomUUID())
                .course(course)
                .title("Module 1")
                .build();

        lesson = Lesson.builder()
                .id(lessonId)
                .module(module)
                .title("Lesson 1")
                .build();

        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(student.getId(), courseId))
                .thenReturn(Optional.of(enrollment));
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(lesson));

        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setCompleted(true);
        request.setLastPosition(30);

        assertThatThrownBy(() -> enrollmentService.updateProgress(courseId, lessonId, request))
                .isInstanceOf(AulixException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void getMyEnrollmentsReturnsUserEnrollments() {
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .user(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .amountPaid(BigDecimal.ZERO)
                .build();

        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(student.getId()))
                .thenReturn(List.of(enrollment));
        when(progressRepository.countCompletedByEnrollmentId(enrollment.getId()))
                .thenReturn(0L);

        List<EnrollmentResponse> result = enrollmentService.getMyEnrollments();

        assertThat(result).hasSize(1);
    }
}