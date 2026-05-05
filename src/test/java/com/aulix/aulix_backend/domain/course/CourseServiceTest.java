package com.aulix.aulix_backend.domain.course;

import com.aulix.aulix_backend.domain.user.User;
import com.aulix.aulix_backend.domain.user.UserRepository;
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
}