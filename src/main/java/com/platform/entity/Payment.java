package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Telegram user ID — bot orqali yuborilgan */
    private Long telegramId;

    /** To'lov miqdori (so'm) */
    @Column(nullable = false)
    private Long amount;

    /** Foydalanuvchi yuborgan chek rasmi URL yoki Telegram file_id */
    @Column(columnDefinition = "TEXT")
    private String receiptFileId;

    /** Chek matni/izohi */
    @Column(columnDefinition = "TEXT")
    private String receiptNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    /** Admin kim tasdiqladi */
    private Long approvedByAdminId;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    /** Premium necha kunga beriladi */
    @Builder.Default
    private int premiumDays = 30;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum Status {
        PENDING,   // Kutilmoqda (chek yuborildi)
        APPROVED,  // Admin tasdiqladi → premium ochildi
        REJECTED   // Admin rad etdi
    }
}