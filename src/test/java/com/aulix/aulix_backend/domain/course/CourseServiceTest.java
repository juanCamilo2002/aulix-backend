package com.aulix.aulix_backend.domain.course;

import com.aulix.aulix_backend.domain.enrollment.EnrollmentRepository;
import com.aulix.aulix_backend.domain.course.dto.PublicLessonResponse;
import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
import com.aulix.aulix_backend.shared.exception.AulixException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        instructor = User.builder()
                .email("instructor@example.com")
                .password("hashed")
                .fullName("Instructor")
                .build();
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
}
