package com.platform.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private Long id;
        private Long userId;
        private String userFullName;
        private String userPhoneNumber;
        private Long telegramId;
        private Long amount;
        private String receiptFileId;
        private String receiptNote;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
        private int premiumDays;
    }

    @Data
    public static class ReviewRequest {
        private boolean approve;   // true = tasdiqlash, false = rad etish
        private String note;       // ixtiyoriy izoh
    }
}