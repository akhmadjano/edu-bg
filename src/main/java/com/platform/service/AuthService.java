package com.platform.service;

import com.platform.config.JwtService;
import com.platform.dto.AuthDto;
import com.platform.entity.User;
import com.platform.exception.BadRequestException;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final JwtService            jwtService;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // ── Ro'yxatdan o'tish ───────────────────────────────────────

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .active(true)
                .build();
        User saved = userRepository.save(user);
        log.info("Yangi foydalanuvchi ro'yxatdan o'tdi: {}", saved.getPhoneNumber());
        return buildResponse(saved);
    }

    // ── Kirish ──────────────────────────────────────────────────

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BadRequestException("Foydalanuvchi topilmadi"));
        log.info("Foydalanuvchi kirdi: {}", user.getPhoneNumber());
        return buildResponse(user);
    }

    // ── Token yangilash ─────────────────────────────────────────

    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String phoneNumber = jwtService.extractUsername(request.getRefreshToken());
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("Foydalanuvchi topilmadi"));
        return buildResponse(user);
    }

    // ── Parol o'zgartirish ──────────────────────────────────────

    @Transactional
    public void changePassword(AuthDto.ChangePasswordRequest request, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("Foydalanuvchi topilmadi"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Eski parol noto'g'ri");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Parol o'zgartirildi: {}", phoneNumber);
    }

    // ── Telegram unlink (bot bog'lanishini o'chirish) ───────────

    @Transactional
    public AuthDto.UserInfo unlinkTelegram(User user) {
        user.setTelegramId(null);
        userRepository.save(user);
        log.info("Telegram bog'lanish o'chirildi: {}", user.getPhoneNumber());
        return mapToUserInfo(user);
    }

    // ── Admin: foydalanuvchi boshqarish ─────────────────────────

    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Foydalanuvchi topilmadi"));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    public void makeAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Foydalanuvchi topilmadi"));
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
    }

    // ── Helpers ─────────────────────────────────────────────────

    private AuthDto.AuthResponse buildResponse(User user) {
        return AuthDto.AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .user(mapToUserInfo(user))
                .build();
    }

    public AuthDto.UserInfo mapToUserInfo(User user) {
        return AuthDto.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .isPremium(user.hasActivePremium())
                .premiumUntil(user.getPremiumUntil())
                .telegramLinked(user.getTelegramId() != null)
                .build();
    }
}