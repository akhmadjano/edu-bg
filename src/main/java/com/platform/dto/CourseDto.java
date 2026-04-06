package com.platform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class CourseDto {

    @Data
    public static class CourseCreateRequest {
        @NotBlank private String title;
        private String description;
        private String thumbnailUrl;
    }

    @Data
    public static class CourseUpdateRequest {
        private String title;
        private String description;
        private String thumbnailUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CourseResponse {
        private Long id;
        private String title;
        private String description;
        private String thumbnailUrl;
        private int totalSections;
        private int totalLessons;
        private List<SectionResponse> sections;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SectionCreateRequest {
        @NotBlank private String title;
        private String description;
        @NotNull @Min(1) private Integer orderIndex;
        @NotNull private Long courseId;
    }

    @Data
    public static class SectionUpdateRequest {
        private String title;
        private String description;
        private Integer orderIndex;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionResponse {
        private Long id;
        private String title;
        private String description;
        private Integer orderIndex;
        private int totalLessons;
        private int passedLessons;
        private boolean isUnlocked;
        private boolean finalTestAvailable;
        private boolean finalTestPassed;
        private Integer finalTestBestScore;
        private int finalTestAttempts;
        /** Bo'lim final testidagi jami savollar soni */
        private int finalTestQuestions;
        private List<LessonResponse> lessons;
    }

    @Data
    public static class LessonCreateRequest {
        @NotBlank private String title;
        private String description;
        @NotBlank private String videoUrl;
        private List<String> videoUrls;   // bir nechta video (ixtiyoriy)
        private List<String> videoTitles; // video sarlavhalari (ixtiyoriy)
        private Integer durationMinutes;
        @NotNull @Min(1) private Integer orderIndex;
        private boolean isFree;
        @NotNull private Long sectionId;
    }

    @Data
    public static class LessonUpdateRequest {
        private String title;
        private String description;
        private String videoUrl;
        private List<String> videoUrls;   // bir nechta video yangilash
        private List<String> videoTitles;
        private Integer durationMinutes;
        private Integer orderIndex;
        private Boolean isFree;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LessonResponse {
        private Long id;
        private String title;
        private String description;
        private String videoUrl;      // null bo'lsa lock (birinchi video yoki yagona)
        private List<String> videoUrls; // barcha videolar ro'yxati (null bo'lsa videoUrl ishlatiladi)
        private List<String> videoTitles; // har bir videoning sarlavhasi (ixtiyoriy)
        private Integer durationMinutes;
        private Integer orderIndex;
        private boolean isFree;
        private boolean isLocked;
        private boolean isPremiumLocked; // login bor lekin premium yo'q
        private boolean videoWatched;
        private boolean testPassed;
        private Integer bestScore;
        private int testAttempts;
        private int totalQuestions;
    }

    // ── SAVOLLAR ────────────────────────────────────────────────

    /** Foydalanuvchiga beriladigan savol — to'g'ri javobsiz! */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionResponse {
        private Long id;
        private String questionText;
        private String imageUrl;
        private List<String> options;   // ["Java", "Python", "C++"]
        // correctAnswers YO'Q — xavfsizlik uchun
    }

    /** Bitta dars savoli qo'shish */
    @Data
    public static class LessonQuestionRequest {
        @NotBlank private String questionText;
        private String imageUrl;
        @NotEmpty @Size(min = 2) private List<String> options;
        @NotEmpty private List<String> correctAnswers;
        @NotNull private Long lessonId;
    }

    /** Bitta final savol qo'shish */
    @Data
    public static class SectionFinalQuestionRequest {
        @NotBlank private String questionText;
        private String imageUrl;
        @NotEmpty @Size(min = 2) private List<String> options;
        @NotEmpty private List<String> correctAnswers;
        @NotNull private Long sectionId;
    }

    /** JSON bulk import — ko'p savolni bir yo'la */
    @Data
    public static class BulkImportRequest {
        private Long lessonId;    // dars testi uchun
        private Long sectionId;   // final test uchun
        @NotEmpty private List<QuestionImportItem> questions;
    }

    /**
     * JSON import item — ikkala formatni qo'llab-quvvatlaydi:
     *
     * 1) Oddiy format (eski):
     * {
     *   "questionText": "Savol matni?",
     *   "imageUrl": null,
     *   "options": ["A javob", "B javob", "C javob"],
     *   "correctAnswers": ["A javob"]
     * }
     *
     * 2) JSON fayl formati (yangi) — Eng_oxirgi_61_4.json kabi:
     * {
     *   "id": "1.1",
     *   "topic_id": "1.9",
     *   "question_text": "Savol matni?",
     *   "image": "rasm.png yoki bo'sh string",
     *   "answers": [
     *     { "option": "A", "text": "Javob matni", "gif": "" },
     *     { "option": "B", "text": "Javob matni", "gif": "" }
     *   ],
     *   "correct_answer": "To'g'ri javob matni"
     * }
     */
    @Data
    public static class QuestionImportItem {
        // ── Oddiy format maydonlari ──────────────────────────────
        private String questionText;
        private String imageUrl;
        private List<String> options;
        private List<String> correctAnswers;

        // ── JSON fayl formati maydonlari ─────────────────────────
        private String id;
        private String topic_id;
        private String question_text;
        private String image;
        private List<AnswerOption> answers;
        private String correct_answer;
    }

    /** JSON fayl formatidagi bitta javob varianti */
    @Data
    public static class AnswerOption {
        private String option;   // "A", "B", "C", "D", "E"
        private String text;     // Javob matni
        private String gif;      // GIF URL yoki bo'sh string
    }
}