package com.platform.controller;

import com.platform.dto.AuthDto;
import com.platform.dto.CourseDto;
import com.platform.dto.PaymentDto;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.service.AuthService;
import com.platform.service.CourseService;
import com.platform.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "3. Admin Panel")
public class AdminController {

    private final CourseService  courseService;
    private final AuthService    authService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    // ── KURS ─────────────────────────────────────────────────────

    @PostMapping("/course")
    public ResponseEntity<CourseDto.CourseResponse> createCourse(@Valid @RequestBody CourseDto.CourseCreateRequest req) {
        return ResponseEntity.status(201).body(courseService.createCourse(req));
    }

    @PutMapping("/course/{id}")
    public ResponseEntity<CourseDto.CourseResponse> updateCourse(@PathVariable Long id, @RequestBody CourseDto.CourseUpdateRequest req) {
        return ResponseEntity.ok(courseService.updateCourse(id, req));
    }

    // ── BO'LIM ───────────────────────────────────────────────────

    @PostMapping("/sections")
    public ResponseEntity<CourseDto.SectionResponse> createSection(@Valid @RequestBody CourseDto.SectionCreateRequest req) {
        return ResponseEntity.status(201).body(courseService.createSection(req));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<CourseDto.SectionResponse> updateSection(@PathVariable Long id, @RequestBody CourseDto.SectionUpdateRequest req) {
        return ResponseEntity.ok(courseService.updateSection(id, req));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<String> deleteSection(@PathVariable Long id) {
        courseService.deleteSection(id); return ResponseEntity.ok("O'chirildi");
    }

    // ── DARS ─────────────────────────────────────────────────────

    @PostMapping("/lessons")
    public ResponseEntity<CourseDto.LessonResponse> createLesson(@Valid @RequestBody CourseDto.LessonCreateRequest req) {
        return ResponseEntity.status(201).body(courseService.createLesson(req));
    }

    @PutMapping("/lessons/{id}")
    public ResponseEntity<CourseDto.LessonResponse> updateLesson(@PathVariable Long id, @RequestBody CourseDto.LessonUpdateRequest req) {
        return ResponseEntity.ok(courseService.updateLesson(id, req));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<String> deleteLesson(@PathVariable Long id) {
        courseService.deleteLesson(id); return ResponseEntity.ok("O'chirildi");
    }

    // ── DARS SAVOLLARI ───────────────────────────────────────────

    @PostMapping("/lesson-questions")
    @Operation(summary = "Bitta savol qo'shish")
    public ResponseEntity<CourseDto.QuestionResponse> addLessonQuestion(
            @Valid @RequestBody CourseDto.LessonQuestionRequest req) {
        return ResponseEntity.status(201).body(courseService.addLessonQuestion(req));
    }

    @DeleteMapping("/lesson-questions/{id}")
    public ResponseEntity<String> deleteLessonQuestion(@PathVariable Long id) {
        courseService.deleteLessonQuestion(id); return ResponseEntity.ok("O'chirildi");
    }

    @PostMapping("/lesson-questions/bulk")
    @Operation(summary = "JSON bulk import — dars testi uchun",
            description = """
        Ikkala formatni qo'llab-quvvatlaydi:

        ── Format 1 (oddiy) ─────────────────────────────────────
        {
          "lessonId": 1,
          "questions": [
            {
              "questionText": "Java nima?",
              "imageUrl": null,
              "options": ["Dasturlash tili", "Kafe", "Qurilma"],
              "correctAnswers": ["Dasturlash tili"]
            }
          ]
        }

        ── Format 2 (JSON fayl strukturasi) ─────────────────────
        {
          "lessonId": 1,
          "questions": [
            {
              "id": "1.1",
              "topic_id": "1.9",
              "question_text": "Savol matni?",
              "image": "",
              "answers": [
                { "option": "A", "text": "Birinchi javob", "gif": "" },
                { "option": "B", "text": "Ikkinchi javob", "gif": "" }
              ],
              "correct_answer": "Birinchi javob"
            }
          ]
        }
        """)
    public ResponseEntity<Map<String, Object>> bulkLessonQuestions(@Valid @RequestBody CourseDto.BulkImportRequest req) {
        int count = courseService.bulkImportLessonQuestions(req);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " ta savol qo'shildi"));
    }

    // ── FINAL TEST SAVOLLARI ─────────────────────────────────────

    @PostMapping("/section-final-questions")
    public ResponseEntity<CourseDto.QuestionResponse> addFinalQuestion(
            @Valid @RequestBody CourseDto.SectionFinalQuestionRequest req) {
        return ResponseEntity.status(201).body(courseService.addSectionFinalQuestion(req));
    }

    @DeleteMapping("/section-final-questions/{id}")
    public ResponseEntity<String> deleteFinalQuestion(@PathVariable Long id) {
        courseService.deleteSectionFinalQuestion(id); return ResponseEntity.ok("O'chirildi");
    }

    @PostMapping("/section-final-questions/bulk")
    @Operation(summary = "JSON bulk import — bo'lim final testi uchun")
    public ResponseEntity<Map<String, Object>> bulkFinalQuestions(@Valid @RequestBody CourseDto.BulkImportRequest req) {
        int count = courseService.bulkImportFinalQuestions(req);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " ta savol qo'shildi"));
    }

    // ── FOYDALANUVCHILAR ─────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(Map.of(
                "content", users.getContent().stream().map(authService::mapToUserInfo).toList(),
                "totalElements", users.getTotalElements(),
                "totalPages", users.getTotalPages(),
                "currentPage", page));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<String> toggleActive(@PathVariable Long id) {
        authService.toggleUserActive(id); return ResponseEntity.ok("Holat o'zgartirildi");
    }

    @PatchMapping("/users/{id}/make-admin")
    public ResponseEntity<String> makeAdmin(@PathVariable Long id) {
        authService.makeAdmin(id); return ResponseEntity.ok("Admin qilindi");
    }

    @PatchMapping("/users/{id}/toggle-premium")
    public ResponseEntity<String> togglePremium(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.platform.exception.NotFoundException("Foydalanuvchi topilmadi"));
        user.setPremium(!user.isPremium());
        if (user.isPremium() && user.getPremiumUntil() == null) {
            user.setPremiumUntil(java.time.LocalDateTime.now().plusDays(30));
        }
        userRepository.save(user);
        return ResponseEntity.ok(user.isPremium() ? "Premium berildi" : "Premium olib tashlandi");
    }

    // ── TO'LOVLAR ────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "all") String status) {
        if ("pending".equals(status)) return ResponseEntity.ok(paymentService.getPendingPayments());
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PatchMapping("/payments/{id}/review")
    @Operation(summary = "To'lovni tasdiqlash yoki rad etish")
    public ResponseEntity<PaymentDto.PaymentResponse> reviewPayment(
            @PathVariable Long id,
            @RequestBody PaymentDto.ReviewRequest req,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(paymentService.reviewPayment(id, admin.getId(), req.isApprove(), req.getNote()));
    }
}