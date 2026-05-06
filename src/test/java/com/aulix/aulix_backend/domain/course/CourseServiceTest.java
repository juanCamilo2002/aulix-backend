package com.aulix.aulix_backend.domain.course;

import com.aulix.aulix_backend.domain.enrollment.EnrollmentRepository;
import com.aulix.aulix_backend.domain.course.dto.PublicLessonResponse;
import com.aulix.aulix_backend.domain.course.dto.CreateLessonRequest;
import com.aulix.aulix_backend.domain.user.Role;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.shared.exception.AulixException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    private User instructor;
    private User otherInstructor;
    private User admin;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        instructor = User.builder()
                .id(UUID.randomUUID())
                .email("instructor@example.com")
                .password("hashed")
                .fullName("Instructor")
                .role(Role.INSTRUCTOR)
                .build();
        otherInstructor = User.builder()
                .id(UUID.randomUUID())
                .email("other-instructor@example.com")
                .password("hashed")
                .fullName("Other Instructor")
                .role(Role.INSTRUCTOR)
                .build();
        admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .password("hashed")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("superadmin@example.com")
                .password("hashed")
                .fullName("Super Admin")
                .role(Role.SUPERADMIN)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPublishedCoursesReturnsOnlyPublished() {
        Course publishedCourse = Course.builder()
                .title("Published Course")
                .slug("published-course")
                .published(true)
                .instructor(instructor)
                .build();

        when(courseRepository.findPublishedWithInstructor())
                .thenReturn(List.of(publishedCourse));

        assertThat(courseService.getPublishedCourses()).hasSize(1);
    }

    @Test
    void getCourseBySlugReturnsPublicOutlineWithoutPrivateContent() {
        Lesson lesson = Lesson.builder()
                .id(UUID.randomUUID())
                .title("Private video")
                .type(LessonType.VIDEO)
                .videoUrl("https://cdn.example.com/private-video.mp4")
                .durationSecs(120)
                .build();
        Module module = Module.builder()
                .id(UUID.randomUUID())
                .title("Module 1")
                .lessons(List.of(lesson))
                .build();
        Course publishedCourse = Course.builder()
                .title("Published Course")
                .slug("published-course")
                .published(true)
                .instructor(instructor)
                .modules(List.of(module))
                .build();

        when(courseRepository.findBySlug("published-course"))
                .thenReturn(Optional.of(publishedCourse));

        var response = courseService.getCourseBySlug("published-course");

        assertThat(response.getModules()).hasSize(1);
        assertThat(response.getModules().getFirst().getLessons()).hasSize(1);
        assertThat(response.getModules().getFirst().getLessons().getFirst())
                .isInstanceOf(PublicLessonResponse.class);
    }

    @Test
    void getCourseBySlugRejectsUnpublishedCourses() {
        Course unpublishedCourse = Course.builder()
                .title("Draft Course")
                .slug("draft-course")
                .published(false)
                .instructor(instructor)
                .build();

        when(courseRepository.findBySlug("draft-course"))
                .thenReturn(Optional.of(unpublishedCourse));

        assertThatThrownBy(() -> courseService.getCourseBySlug("draft-course"))
                .isInstanceOf(AulixException.class)
                .hasMessage("Curso no encontrado: draft-course");
    }

    @Test
    void togglePublishAllowsCourseOwner() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .title("Owner Course")
                .slug("owner-course")
                .published(false)
                .instructor(instructor)
                .build();

        authenticateAs(instructor);
        when(userRepository.findByEmail(instructor.getEmail())).thenReturn(Optional.of(instructor));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        var response = courseService.togglePublish(courseId);

        assertThat(response.isPublished()).isTrue();
        verify(courseRepository).save(course);
    }

    @Test
    void togglePublishRejectsInstructorThatDoesNotOwnCourse() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .title("Other Course")
                .slug("other-course")
                .instructor(instructor)
                .build();

        authenticateAs(otherInstructor);
        when(userRepository.findByEmail(otherInstructor.getEmail())).thenReturn(Optional.of(otherInstructor));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.togglePublish(courseId))
                .isInstanceOf(AulixException.class)
                .hasMessage("No tienes permiso para modificar este curso");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void togglePublishAllowsAdminAndSuperAdmin() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .title("Admin Course")
                .slug("admin-course")
                .published(false)
                .instructor(instructor)
                .build();

        authenticateAs(admin);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        assertThat(courseService.togglePublish(courseId).isPublished()).isTrue();

        authenticateAs(superAdmin);
        when(userRepository.findByEmail(superAdmin.getEmail())).thenReturn(Optional.of(superAdmin));
        course.setPublished(false);

        assertThat(courseService.togglePublish(courseId).isPublished()).isTrue();
    }

    @Test
    void addLessonRejectsInstructorThatDoesNotOwnModuleCourse() {
        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .title("Course")
                .slug("course")
                .instructor(instructor)
                .build();
        Module module = Module.builder()
                .id(moduleId)
                .course(course)
                .build();
        CreateLessonRequest request = new CreateLessonRequest();
        request.setTitle("Lesson");

        authenticateAs(otherInstructor);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findByEmail(otherInstructor.getEmail())).thenReturn(Optional.of(otherInstructor));

        assertThatThrownBy(() -> courseService.addLesson(moduleId, request))
                .isInstanceOf(AulixException.class)
                .hasMessage("No tienes permiso para modificar este curso");

        verify(courseRepository, never()).save(any());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );
    }
}
