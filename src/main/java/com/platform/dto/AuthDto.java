package com.platform.dto;

import com.platform.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthDto {

    // ── REQUEST ─────────────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Ism familiya kiritish shart")
        private String fullName;

        @NotBlank(message = "Telefon raqam kiritish shart")
        @Pattern(regexp = "^[+]?[0-9 \\-]{7,20}$", message = "Telefon raqam noto'g'ri formatda (masalan: +998901234567)")
        private String phoneNumber;

        @NotBlank(message = "Parol kiritish shart")
        @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Telefon raqam kiritish shart")
        private String phoneNumber;

        @NotBlank(message = "Parol kiritish shart")
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token kiritish shart")
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Eski parol kiritish shart")
        private String oldPassword;

        @NotBlank(message = "Yangi parol kiritish shart")
        @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
        private String newPassword;
    }

    // ── RESPONSE ────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private UserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String phoneNumber;
        private User.Role role;
        private LocalDateTime createdAt;

        /** Premium obuna faolmi */
        @com.fasterxml.jackson.annotation.JsonProperty("isPremium")
        private boolean isPremium;

        /** Premium tugash sanasi (null = abadiy yoki premium yo'q) */
        private LocalDateTime premiumUntil;

        /** Telegram bot bilan bog'langanmi */
        private boolean telegramLinked;
    }
}