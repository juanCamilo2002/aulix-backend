package com.aulix.aulix_backend.domain.course;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LessonType type = LessonType.VIDEO;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "content_md", columnDefinition = "text")
    private String contentMd;

    @Column(name = "duration_secs")
    @Builder.Default
    private int durationSecs = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
