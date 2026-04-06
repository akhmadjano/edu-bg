package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Video URL: YouTube embed URL yoki o'z serveringizdagi URL.
     * Misol: https://www.youtube.com/embed/xxxxx
     * Eski maydon — birinchi video saqlanadi (backward compat).
     */
    @Column(nullable = false)
    private String videoUrl;

    /**
     * Bir nechta video URL — JSON array ko'rinishida saqlanadi.
     * Misol: ["https://youtube.com/embed/aaa", "https://youtube.com/embed/bbb"]
     * Bo'sh bo'lsa, videoUrl ishlatiladi.
     */
    @Column(columnDefinition = "TEXT")
    private String videoUrls;

    // Dars davomiyligi (minutda)
    private Integer durationMinutes;

    // Bo'limdagi tartib raqami (1 dan boshlanadi)
    @Column(nullable = false)
    private Integer orderIndex;

    /**
     * TRUE  → Hamma (login qilmagan ham) ko'ra oladi - birinchi dars
     * FALSE → Ko'rish uchun oldingi dars testi 80%+ bo'lishi kerak
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isFree = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // Dars savollari
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LessonQuestion> questions = new ArrayList<>();
}