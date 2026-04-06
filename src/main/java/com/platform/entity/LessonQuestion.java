package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** Rasm URL — ixtiyoriy */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Variantlar — JSON array saqlash:
     * ["Java", "Python", "C++", "JavaScript"]
     * Istalgancha (2 dan N gacha) variant bo'lishi mumkin
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String options;

    /**
     * To'g'ri javoblar — JSON array:
     * ["Java"] yoki ["Java", "Python"]
     * Matn bo'yicha tekshiriladi (case-insensitive)
     * Foydalanuvchiga HECH QACHON yuborilmaydi!
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String correctAnswers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
}