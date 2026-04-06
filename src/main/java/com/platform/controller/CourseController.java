package com.platform.controller;

import com.platform.dto.CourseDto;
import com.platform.dto.TestDto;
import com.platform.entity.User;
import com.platform.service.CourseService;
import com.platform.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Tag(name = "2. Kurs", description = "Kurs ko'rish, dars tomosha qilish, test topshirish")
public class CourseController {

    private final CourseService   courseService;
    private final ProgressService progressService;

    // ============================================================
    // KURSNI KO'RISH
    // ============================================================

    @GetMapping("/overview")
    @Operation(
            summary = "Kurs tarkibi (public)",
            description = """
            Login qilmagan foydalanuvchilar ham ko'ra oladi.
            Lekin bepul darsdan tashqari barcha darslar `isLocked=true` va `videoUrl=null` bo'ladi.
            """
    )
    public ResponseEntity<CourseDto.CourseResponse> getCourseOverview(
            @AuthenticationPrincipal User user) {
        Long userId = (user != null) ? user.getId() : null;
        return ResponseEntity.ok(courseService.getCourseOverview(userId));
    }

    @GetMapping("/my-progress")
    @Operation(summary = "Mening progressim",
            description = "Foydalanuvchining bo'limlar va darslar bo'yicha to'liq progressi")
    public ResponseEntity<TestDto.UserProgressResponse> myProgress(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.getUserProgress(user.getId()));
    }

    // ============================================================
    // VIDEO KO'RISH
    // ============================================================

    @PostMapping("/lessons/{lessonId}/watch")
    @Operation(
            summary = "Videoni ko'rdim",
            description = """
            Videoni tomosha qilgach shu endpointni chaqiring.
            Bu amal videoni ko'rilgan deb belgilaydi va dars testiga kirish imkonini ochadi.
            
            SHART: Bu darsni ko'rish huquqi bo'lishi kerak (oldingi dars testi o'tilgan).
            """
    )
    public ResponseEntity<String> markWatched(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        progressService.markVideoWatched(user.getId(), lessonId);
        return ResponseEntity.ok("Video ko'rildi. Endi testni topshirishingiz mumkin!");
    }

    // ============================================================
    // DARS TESTI
    // ============================================================

    @GetMapping("/lessons/{lessonId}/test")
    @Operation(
            summary = "Dars test savollarini olish",
            description = """
            Dars testining A, B, C, D variantli savollarini olish.
            TO'G'RI JAVOB ko'rsatilmaydi — faqat savollar va variantlar.
            
            SHART: Avval `POST /lessons/{id}/watch` ni chaqirgan bo'lish kerak.
            """
    )
    public ResponseEntity<List<CourseDto.QuestionResponse>> getLessonTest(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(courseService.getLessonQuestions(lessonId, user.getId()));
    }

    @PostMapping("/lessons/{lessonId}/test")
    @Operation(
            summary = "Dars testini topshirish",
            description = """
            Javoblarni yuborish va natijani olish.
            
            80%+ → ✅ Keyingi dars ochildi!
            80% dan kam → ❌ Qaytadan urinib ko'ring (cheksiz urinish huquqi)
            
            Request body misoli:
            ```json
            {
              "answers": {
                "1": "A",
                "2": "C",
                "3": "B",
                "4": "D"
              }
            }
            ```
            Bu yerda `1`, `2`, `3`, `4` — savol IDlari (GET endpointdan olinadi).
            """
    )
    public ResponseEntity<TestDto.TestResultResponse> submitLessonTest(
            @PathVariable Long lessonId,
            @Valid @RequestBody TestDto.SubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                progressService.submitLessonTest(user.getId(), lessonId, request.getAnswers()));
    }

    // ============================================================
    // BO'LIM FINAL TESTI
    // ============================================================

    @GetMapping("/sections/{sectionId}/final-test")
    @Operation(
            summary = "Bo'lim final test savollarini olish",
            description = """
            Bo'limdagi BARCHA darslar testlari o'tilgandan keyin ochiladi.
            TO'G'RI JAVOB ko'rsatilmaydi.
            """
    )
    public ResponseEntity<List<CourseDto.QuestionResponse>> getSectionFinalTest(
            @PathVariable Long sectionId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(courseService.getSectionFinalQuestions(sectionId, user.getId()));
    }

    @PostMapping("/sections/{sectionId}/final-test")
    @Operation(
            summary = "Bo'lim final testini topshirish",
            description = """
            80%+ → ✅ Bo'lim yakunlandi! Keyingi bo'lim ochildi.
            80% dan kam → ❌ Qaytadan urinib ko'ring (cheksiz urinish huquqi)
            """
    )
    public ResponseEntity<TestDto.TestResultResponse> submitSectionFinalTest(
            @PathVariable Long sectionId,
            @Valid @RequestBody TestDto.SubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                progressService.submitSectionFinalTest(user.getId(), sectionId, request.getAnswers()));
    }
}