package com.platform.service;

import com.platform.dto.TestDto;
import com.platform.entity.*;
import com.platform.exception.BadRequestException;
import com.platform.exception.ForbiddenException;
import com.platform.exception.NotFoundException;
import com.platform.repository.*;
import com.platform.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository                 userRepo;
    private final LessonRepository               lessonRepo;
    private final SectionRepository              sectionRepo;
    private final LessonQuestionRepository       lessonQRepo;
    private final SectionFinalQuestionRepository sectionFQRepo;
    private final UserLessonProgressRepository   lessonProgressRepo;
    private final UserSectionProgressRepository  sectionProgressRepo;
    private final CourseService                  courseService;
    private final JsonUtil                       jsonUtil;

    @Value("${course.lesson-pass-score:80}")
    private int lessonPassScore;

    @Value("${course.section-pass-score:80}")
    private int sectionPassScore;

    // ==========================================================
    // VIDEO KO'RILDI
    // ==========================================================

    @Transactional
    public void markVideoWatched(Long userId, Long lessonId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));

        User user = userRepo.findById(userId).orElseThrow();
        boolean hasPremium = user.hasActivePremium();
        boolean isFirstSection = lesson.getSection().getOrderIndex() == 1;
        boolean isFirstLesson = lesson.getOrderIndex() == 1 && isFirstSection;

        // Premium yo'q foydalanuvchi faqat 1-bo'lim 1-darsini ko'ra oladi
        if (!isFirstLesson && !hasPremium) {
            throw new ForbiddenException("Bu darsni ko'rish uchun premium kerak");
        }
        // Premium bor, lekin bo'lim ochiq emas
        if (!courseService.isSectionUnlocked(userId, lesson.getSection())) {
            throw new ForbiddenException("Bu bo'lim hali ochilmagan");
        }
        // Premium bor, oldingi dars testi o'tilmagan
        if (!isFirstLesson && !courseService.canWatchLesson(userId, lesson)) {
            throw new ForbiddenException("Oldingi dars testini toping (80%+)");
        }

        UserLessonProgress prog = lessonProgressRepo
                .findByUserIdAndLessonId(userId, lessonId)
                .orElse(UserLessonProgress.builder().user(user).lesson(lesson).build());
        if (!prog.isVideoWatched()) {
            prog.setVideoWatched(true);
            prog.setVideoWatchedAt(LocalDateTime.now());
            lessonProgressRepo.save(prog);
        }
    }

    // ==========================================================
    // DARS TESTINI TOPSHIRISH
    // Javob tekshirish: matn bo'yicha (case-insensitive)
    // ==========================================================

    @Transactional
    public TestDto.TestResultResponse submitLessonTest(Long userId, Long lessonId,
                                                       Map<Long, String> answers) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));

        UserLessonProgress prog = lessonProgressRepo
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new BadRequestException("Avval videoni ko'ring!"));

        if (!prog.isVideoWatched()) {
            throw new BadRequestException("Testni topshirish uchun avval videoni tomosha qiling");
        }

        List<LessonQuestion> questions = lessonQRepo.findByLessonId(lessonId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Bu dars uchun test savollari qo'shilmagan");
        }

        TestResult result = checkLessonAnswers(questions, answers);

        prog.setTestAttempts(prog.getTestAttempts() + 1);
        if (prog.getBestScore() == null || result.scorePercent > prog.getBestScore()) {
            prog.setBestScore(result.scorePercent);
        }
        boolean passed = result.scorePercent >= lessonPassScore;
        if (passed && !prog.isTestPassed()) {
            prog.setTestPassed(true);
            prog.setTestPassedAt(LocalDateTime.now());
        }
        lessonProgressRepo.save(prog);

        String msg = passed
                ? "🎉 Tabriklaymiz! " + result.scorePercent + "% — Keyingi dars ochildi!"
                : "❌ " + result.scorePercent + "% — O'tish uchun " + lessonPassScore + "% kerak.";

        return buildResult(result, passed, msg, lessonPassScore);
    }

    // ==========================================================
    // BO'LIM FINAL TESTINI TOPSHIRISH
    // ==========================================================

    @Transactional
    public TestDto.TestResultResponse submitSectionFinalTest(Long userId, Long sectionId,
                                                             Map<Long, String> answers) {
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));

        if (!courseService.isSectionUnlocked(userId, section)) {
            throw new ForbiddenException("Bu bo'lim hali ochilmagan");
        }

        boolean allPassed = lessonProgressRepo.areAllLessonsPassedInSection(userId, sectionId);
        if (!allPassed) {
            long passed = lessonProgressRepo.countPassedLessonsInSection(userId, sectionId);
            long total  = lessonRepo.countBySectionId(sectionId);
            throw new BadRequestException("Barcha darslar testini toping: " + passed + "/" + total);
        }

        List<SectionFinalQuestion> questions = sectionFQRepo.findBySectionId(sectionId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Bu bo'lim uchun final test savollari qo'shilmagan");
        }

        TestResult result = checkFinalAnswers(questions, answers);

        User user = userRepo.findById(userId).orElseThrow();
        UserSectionProgress sp = sectionProgressRepo
                .findByUserIdAndSectionId(userId, sectionId)
                .orElse(UserSectionProgress.builder().user(user).section(section).build());

        sp.setFinalTestAttempts(sp.getFinalTestAttempts() + 1);
        if (sp.getBestScore() == null || result.scorePercent > sp.getBestScore()) sp.setBestScore(result.scorePercent);

        boolean passed = result.scorePercent >= sectionPassScore;
        if (passed && !sp.isFinalTestPassed()) {
            sp.setFinalTestPassed(true);
            sp.setFinalTestPassedAt(LocalDateTime.now());
        }
        sectionProgressRepo.save(sp);

        String msg = passed
                ? "🏆 Bo'lim yakunlandi! Keyingi bo'lim ochildi!"
                : "❌ " + result.scorePercent + "% — O'tish uchun " + sectionPassScore + "% kerak.";

        return buildResult(result, passed, msg, sectionPassScore);
    }

    // ==========================================================
    // PROGRESS
    // ==========================================================

    public TestDto.UserProgressResponse getUserProgress(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));
        List<Section> sections = sectionRepo.findAll().stream()
                .filter(s -> s.getCourse().isActive())
                .sorted(Comparator.comparing(Section::getOrderIndex)).toList();

        List<TestDto.SectionProgressInfo> infos = new ArrayList<>();
        int totalLessons = 0, watchedLessons = 0, passedTests = 0, passedSections = 0;

        for (Section s : sections) {
            boolean unlocked = courseService.isSectionUnlocked(userId, s);
            long total   = lessonRepo.countBySectionId(s.getId());
            long passed  = lessonProgressRepo.countPassedLessonsInSection(userId, s.getId());
            long watched = lessonProgressRepo.findByUserIdAndLesson_Section_Id(userId, s.getId())
                    .stream().filter(UserLessonProgress::isVideoWatched).count();
            boolean fpassed = sectionProgressRepo.existsByUserIdAndSectionIdAndFinalTestPassedTrue(userId, s.getId());
            Integer fbest = sectionProgressRepo.findByUserIdAndSectionId(userId, s.getId())
                    .map(UserSectionProgress::getBestScore).orElse(null);

            totalLessons   += (int) total;
            watchedLessons += (int) watched;
            passedTests    += (int) passed;
            if (fpassed) passedSections++;

            infos.add(TestDto.SectionProgressInfo.builder()
                    .sectionId(s.getId()).sectionTitle(s.getTitle()).orderIndex(s.getOrderIndex())
                    .isUnlocked(unlocked).totalLessons((int) total).passedLessons((int) passed)
                    .finalTestPassed(fpassed).finalTestBestScore(fbest).build());
        }

        int overall = totalLessons == 0 ? 0 : (passedTests * 100 / totalLessons);
        return TestDto.UserProgressResponse.builder()
                .userId(userId).fullName(user.getFullName())
                .isPremium(user.hasActivePremium())
                .totalLessons(totalLessons).watchedLessons(watchedLessons)
                .passedLessonTests(passedTests).totalSections(sections.size())
                .passedSections(passedSections).overallProgressPercent(overall)
                .sections(infos).build();
    }

    // ==========================================================
    // JAVOB TEKSHIRISH — matn bo'yicha (case-insensitive)
    // ==========================================================

    private TestResult checkLessonAnswers(List<LessonQuestion> questions, Map<Long, String> answers) {
        // Frontend faqat tanlangan 25 ta savolni yuboradi — faqat javob berilgan savollar bo'yicha hisoblash
        List<LessonQuestion> answeredQuestions = questions.stream()
                .filter(q -> answers.containsKey(q.getId()))
                .toList();
        // Agar backend barcha savollarni tekshirsin desa — barcha questions ishlatiladi
        // Lekin biz faqat berilgan savollar (25 ta) bo'yicha hisoblashimiz kerak
        List<LessonQuestion> toCheck = answeredQuestions.isEmpty() ? questions : answeredQuestions;

        int correct = 0;
        List<TestDto.QuestionResult> details = new ArrayList<>();
        for (LessonQuestion q : toCheck) {
            String ua = answers.getOrDefault(q.getId(), "").trim();
            List<String> correctList = jsonUtil.fromJson(q.getCorrectAnswers());
            boolean ok = correctList.stream().anyMatch(ca -> ca.equalsIgnoreCase(ua));
            if (ok) correct++;
            details.add(TestDto.QuestionResult.builder()
                    .questionId(q.getId()).questionText(q.getQuestionText())
                    .userAnswer(ua.isEmpty() ? "Javob berilmagan" : ua)
                    .correctAnswers(correctList).isCorrect(ok).build());
        }
        int pct = toCheck.isEmpty() ? 0 : (correct * 100 / toCheck.size());
        return new TestResult(toCheck.size(), correct, pct, details);
    }

    private TestResult checkFinalAnswers(List<SectionFinalQuestion> questions, Map<Long, String> answers) {
        // Frontend faqat tanlangan 25 ta savolni yuboradi
        List<SectionFinalQuestion> answeredQuestions = questions.stream()
                .filter(q -> answers.containsKey(q.getId()))
                .toList();
        List<SectionFinalQuestion> toCheck = answeredQuestions.isEmpty() ? questions : answeredQuestions;

        int correct = 0;
        List<TestDto.QuestionResult> details = new ArrayList<>();
        for (SectionFinalQuestion q : toCheck) {
            String ua = answers.getOrDefault(q.getId(), "").trim();
            List<String> correctList = jsonUtil.fromJson(q.getCorrectAnswers());
            boolean ok = correctList.stream().anyMatch(ca -> ca.equalsIgnoreCase(ua));
            if (ok) correct++;
            details.add(TestDto.QuestionResult.builder()
                    .questionId(q.getId()).questionText(q.getQuestionText())
                    .userAnswer(ua.isEmpty() ? "Javob berilmagan" : ua)
                    .correctAnswers(correctList).isCorrect(ok).build());
        }
        int pct = toCheck.isEmpty() ? 0 : (correct * 100 / toCheck.size());
        return new TestResult(toCheck.size(), correct, pct, details);
    }

    private TestDto.TestResultResponse buildResult(TestResult r, boolean passed, String msg, int passScore) {
        return TestDto.TestResultResponse.builder()
                .totalQuestions(r.total).correctAnswers(r.correct)
                .wrongAnswers(r.total - r.correct).scorePercent(r.scorePercent)
                .passed(passed).passingScore(passScore).message(msg).details(r.details).build();
    }

    private record TestResult(int total, int correct, int scorePercent, List<TestDto.QuestionResult> details) {}
}