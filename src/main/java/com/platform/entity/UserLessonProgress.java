package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Har bir user + dars uchun progress.
 *
 * Holat:
 *   videoWatched = false → Dars hali ko'rilmagan
 *   videoWatched = true  → Dars ko'rildi, test topshirish mumkin
 *   testPassed   = true  → Test 80%+ topildi, keyingi dars ochildi
 */
@Entity
@Table(name = "user_lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    @Builder.Default
    private boolean videoWatched = false;

    @Column(nullable = false)
    @Builder.Default
    private int testAttempts = 0;

    // Eng yuqori ball (0-100%)
    private Integer bestScore;

    @Column(nullable = false)
    @Builder.Default
    private boolean testPassed = false;

    private LocalDateTime videoWatchedAt;
    private LocalDateTime testPassedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {}
}