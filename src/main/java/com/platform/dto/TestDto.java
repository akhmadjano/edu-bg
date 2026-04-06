package com.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.Map;

public class TestDto {

    /**
     * Foydalanuvchi javoblarini yuboradi:
     * { "answers": { "12": "Java", "13": "Python" } }
     * Key = questionId (Long), Value = tanlangan variant matni
     */
    @Data
    public static class SubmitRequest {
        @NotEmpty
        private Map<Long, String> answers;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TestResultResponse {
        private int totalQuestions;
        private int correctAnswers;
        private int wrongAnswers;
        private int scorePercent;
        private boolean passed;
        private int passingScore;
        private String message;
        private List<QuestionResult> details;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionResult {
        private Long questionId;
        private String questionText;
        private String userAnswer;
        private List<String> correctAnswers; // natija ko'rsatilganda
        private boolean isCorrect;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserProgressResponse {
        private Long userId;
        private String fullName;
        private boolean isPremium;
        private int totalLessons;
        private int watchedLessons;
        private int passedLessonTests;
        private int totalSections;
        private int passedSections;
        private int overallProgressPercent;
        private List<SectionProgressInfo> sections;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionProgressInfo {
        private Long sectionId;
        private String sectionTitle;
        private Integer orderIndex;
        private boolean isUnlocked;
        private int totalLessons;
        private int passedLessons;
        private boolean finalTestPassed;
        private Integer finalTestBestScore;
    }
}