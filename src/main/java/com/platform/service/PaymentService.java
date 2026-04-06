package com.platform.service;

import com.platform.dto.PaymentDto;
import com.platform.entity.Payment;
import com.platform.entity.User;
import com.platform.exception.NotFoundException;
import com.platform.repository.PaymentRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final UserRepository    userRepo;
    private final TelegramBotService telegramBot;

    /** Telegram bot orqali chek yuborilganda chaqiriladi */
    @Transactional
    public Payment createPayment(Long telegramId, Long amount, String receiptFileId,
                                 String receiptNote, int premiumDays) {
        User user = userRepo.findByTelegramId(telegramId)
                .orElseThrow(() -> new NotFoundException("Telegram ID bilan foydalanuvchi topilmadi. /start bosing."));

        Payment payment = Payment.builder()
                .user(user).telegramId(telegramId)
                .amount(amount).receiptFileId(receiptFileId)
                .receiptNote(receiptNote).premiumDays(premiumDays)
                .status(Payment.Status.PENDING).build();

        Payment saved = paymentRepo.save(payment);
        log.info("Yangi to'lov yaratildi: paymentId={}, userId={}", saved.getId(), user.getId());

        // Adminlarga xabar yuborish (TelegramBotService orqali)
        telegramBot.notifyAdminsNewPayment(saved);
        return saved;
    }

    /** Admin to'lovni tasdiqlaydi yoki rad etadi */
    @Transactional
    public PaymentDto.PaymentResponse reviewPayment(Long paymentId, Long adminId,
                                                    boolean approve, String note) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("To'lov topilmadi"));

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalStateException("Bu to'lov allaqachon ko'rib chiqilgan");
        }

        payment.setStatus(approve ? Payment.Status.APPROVED : Payment.Status.REJECTED);
        payment.setApprovedByAdminId(adminId);
        payment.setReviewedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        if (approve) {
            User user = payment.getUser();
            user.setPremium(true);
            LocalDateTime until = LocalDateTime.now().plusDays(payment.getPremiumDays());
            user.setPremiumUntil(until);
            userRepo.save(user);
            log.info("Premium berildi: userId={}, until={}", user.getId(), until);
            // Foydalanuvchiga Telegram xabar
            if (user.getTelegramId() != null) {
                telegramBot.sendPlain(user.getTelegramId(),
                        "To'lovingiz tasdiqlandi! Premium obuna " + payment.getPremiumDays() +
                                " kunga faollashtirildi. Saytga kiring: 🎉");
            }
        } else {
            if (payment.getTelegramId() != null) {
                telegramBot.sendPlain(payment.getTelegramId(),
                        "Afsuski to'lovingiz tasdiqlanmadi. " +
                                (note != null ? "Sabab: " + note : "") +
                                "\nQayta urinib ko'ring yoki admin bilan bog'laning.");
            }
        }

        return mapToResponse(payment);
    }

    public List<PaymentDto.PaymentResponse> getPendingPayments() {
        return paymentRepo.findByStatusOrderByCreatedAtDesc(Payment.Status.PENDING)
                .stream().map(this::mapToResponse).toList();
    }

    public List<PaymentDto.PaymentResponse> getAllPayments() {
        return paymentRepo.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent().stream().map(this::mapToResponse).toList();
    }

    public PaymentDto.PaymentResponse mapToResponse(Payment p) {
        return PaymentDto.PaymentResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .userFullName(p.getUser().getFullName())
                .userPhoneNumber(p.getUser().getPhoneNumber())
                .telegramId(p.getTelegramId())
                .amount(p.getAmount())
                .receiptFileId(p.getReceiptFileId())
                .receiptNote(p.getReceiptNote())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .reviewedAt(p.getReviewedAt())
                .premiumDays(p.getPremiumDays())
                .build();
    }
}