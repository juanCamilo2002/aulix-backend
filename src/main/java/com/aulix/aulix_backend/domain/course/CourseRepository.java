package com.aulix.aulix_backend.domain.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.published = true ORDER BY c.createdAt DESC")
    List<Course> findPublishedWithInstructor();

    Optional<Course> findBySlug(String slug);

    List<Course> findByInstructorIdOrderByCreatedAtDesc(UUID instructorId);

    boolean existsBySlug(String slug);
}

