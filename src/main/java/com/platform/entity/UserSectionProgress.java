package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Har bir user + bo'lim uchun final test natijasi.
 *
 * finalTestPassed = true → Bo'lim tugadi, keyingi bo'lim ochildi
 */
@Entity
@Table(name = "user_section_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "section_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSectionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    @Builder.Default
    private int finalTestAttempts = 0;

    private Integer bestScore;

    @Column(nullable = false)
    @Builder.Default
    private boolean finalTestPassed = false;

    private LocalDateTime finalTestPassedAt;
}