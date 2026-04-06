package com.platform.repository;

import com.platform.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.Status status);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Payment> findByTelegramIdAndStatus(Long telegramId, Payment.Status status);

    boolean existsByUserIdAndStatus(Long userId, Payment.Status status);
}