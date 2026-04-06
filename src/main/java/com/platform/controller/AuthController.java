package com.platform.controller;

import com.platform.dto.AuthDto;
import com.platform.entity.User;
import com.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autentifikatsiya", description = "Register, Login, Token yangilash")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Ro'yxatdan o'tish")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Tizimga kirish")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access tokenni yangilash")
    public ResponseEntity<AuthDto.AuthResponse> refresh(@Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Mening profilim — isPremium va telegramLinked ni ham qaytaradi")
    public ResponseEntity<AuthDto.UserInfo> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.mapToUserInfo(user));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Parolni o'zgartirish")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest request,
            @AuthenticationPrincipal User user) {
        authService.changePassword(request, user.getPhoneNumber());
        return ResponseEntity.ok("Parol muvaffaqiyatli o'zgartirildi");
    }

    @DeleteMapping("/telegram/unlink")
    @Operation(summary = "Telegram bog'lanishini o'chirish")
    public ResponseEntity<AuthDto.UserInfo> unlinkTelegram(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.unlinkTelegram(user));
    }
}