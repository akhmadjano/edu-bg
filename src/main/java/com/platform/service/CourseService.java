package com.platform.service;

import com.platform.dto.CourseDto;
import com.platform.entity.*;
import com.platform.exception.BadRequestException;
import com.platform.exception.ForbiddenException;
import com.platform.exception.NotFoundException;
import com.platform.repository.*;
import com.platform.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    // Bo'lim ochilishi uchun minimal ball chegarasi (80%)
    private static final int SECTION_UNLOCK_MIN_SCORE = 80;

    private final CourseRepository               courseRepo;
    private final SectionRepository              sectionRepo;
    private final LessonRepository               lessonRepo;
    private final LessonQuestionRepository       lessonQRepo;
    private final SectionFinalQuestionRepository sectionFQRepo;
    private final UserLessonProgressRepository   lessonProgressRepo;
    private final UserSectionProgressRepository  sectionProgressRepo;
    private final UserRepository                 userRepo;
    private final JsonUtil                       jsonUtil;

    // ==========================================================
    // KURS
    // ==========================================================

    public CourseDto.CourseResponse getCourseOverview(Long userId) {
        Course course = getActiveCourse();
        User user = userId != null ? userRepo.findById(userId).orElse(null) : null;
        return mapCourse(course, userId, user);
    }

    @Transactional
    public CourseDto.CourseResponse createCourse(CourseDto.CourseCreateRequest req) {
        Course c = Course.builder()
                .title(req.getTitle()).description(req.getDescription())
                .thumbnailUrl(req.getThumbnailUrl()).isActive(true).build();
        return mapCourse(courseRepo.save(c), null, null);
    }

    @Transactional
    public CourseDto.CourseResponse updateCourse(Long id, CourseDto.CourseUpdateRequest req) {
        Course c = courseRepo.findById(id).orElseThrow(() -> new NotFoundException("Kurs topilmadi"));
        if (req.getTitle()        != null) c.setTitle(req.getTitle());
        if (req.getDescription()  != null) c.setDescription(req.getDescription());
        if (req.getThumbnailUrl() != null) c.setThumbnailUrl(req.getThumbnailUrl());
        return mapCourse(courseRepo.save(c), null, null);
    }

    // ==========================================================
    // BO'LIM
    // ==========================================================

    @Transactional
    public CourseDto.SectionResponse createSection(CourseDto.SectionCreateRequest req) {
        Course course = courseRepo.findById(req.getCourseId())
                .orElseThrow(() -> new NotFoundException("Kurs topilmadi"));
        sectionRepo.findByCourseIdAndOrderIndex(course.getId(), req.getOrderIndex())
                .ifPresent(s -> { throw new BadRequestException(req.getOrderIndex() + "-tartibli bo'lim mavjud"); });
        Section s = Section.builder().title(req.getTitle()).description(req.getDescription())
                .orderIndex(req.getOrderIndex()).course(course).build();
        return mapSection(sectionRepo.save(s), null, null);
    }

    @Transactional
    public CourseDto.SectionResponse updateSection(Long id, CourseDto.SectionUpdateRequest req) {
        Section s = sectionRepo.findById(id).orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));
        if (req.getTitle()       != null) s.setTitle(req.getTitle());
        if (req.getDescription() != null) s.setDescription(req.getDescription());
        if (req.getOrderIndex()  != null) s.setOrderIndex(req.getOrderIndex());
        return mapSection(sectionRepo.save(s), null, null);
    }

    @Transactional
    public void deleteSection(Long id) {
        sectionRepo.delete(sectionRepo.findById(id).orElseThrow(() -> new NotFoundException("Bo'lim topilmadi")));
    }

    // ==========================================================
    // DARS
    // ==========================================================

    @Transactional
    public CourseDto.LessonResponse createLesson(CourseDto.LessonCreateRequest req) {
        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));
        lessonRepo.findBySectionIdAndOrderIndex(section.getId(), req.getOrderIndex())
                .ifPresent(l -> { throw new BadRequestException(req.getOrderIndex() + "-tartibli dars mavjud"); });
        // VideoUrls JSON ga o'girish
        String videoUrlsJson = null;
        if (req.getVideoUrls() != null && !req.getVideoUrls().isEmpty()) {
            List<String> embedded = req.getVideoUrls().stream().map(this::toEmbedUrl).toList();
            videoUrlsJson = jsonUtil.toJson(embedded);
        }
        String videoTitlesJson = null;
        if (req.getVideoTitles() != null && !req.getVideoTitles().isEmpty()) {
            videoTitlesJson = jsonUtil.toJson(req.getVideoTitles());
        }
        Lesson l = Lesson.builder().title(req.getTitle()).description(req.getDescription())
                .videoUrl(toEmbedUrl(req.getVideoUrl())).videoUrls(videoUrlsJson)
                .durationMinutes(req.getDurationMinutes())
                .orderIndex(req.getOrderIndex()).isFree(req.isFree()).section(section).build();
        return mapLesson(lessonRepo.save(l), null, null);
    }

    @Transactional
    public CourseDto.LessonResponse updateLesson(Long id, CourseDto.LessonUpdateRequest req) {
        Lesson l = lessonRepo.findById(id).orElseThrow(() -> new NotFoundException("Dars topilmadi"));
        if (req.getTitle()          != null) l.setTitle(req.getTitle());
        if (req.getDescription()    != null) l.setDescription(req.getDescription());
        if (req.getVideoUrl()       != null) l.setVideoUrl(toEmbedUrl(req.getVideoUrl()));
        if (req.getVideoUrls()      != null) {
            if (req.getVideoUrls().isEmpty()) {
                l.setVideoUrls(null);
            } else {
                List<String> embedded = req.getVideoUrls().stream().map(this::toEmbedUrl).toList();
                l.setVideoUrls(jsonUtil.toJson(embedded));
            }
        }
        if (req.getVideoTitles()    != null) l.setVideoUrls(l.getVideoUrls()); // sarlavhalar alohida maydon yo'q, saqlab qo'yamiz
        if (req.getDurationMinutes()!= null) l.setDurationMinutes(req.getDurationMinutes());
        if (req.getOrderIndex()     != null) l.setOrderIndex(req.getOrderIndex());
        if (req.getIsFree()         != null) l.setFree(req.getIsFree());
        return mapLesson(lessonRepo.save(l), null, null);
    }

    @Transactional
    public void deleteLesson(Long id) {
        lessonRepo.delete(lessonRepo.findById(id).orElseThrow(() -> new NotFoundException("Dars topilmadi")));
    }

    // ==========================================================
    // SAVOLLAR — bitta qo'shish
    // ==========================================================

    @Transactional
    public CourseDto.QuestionResponse addLessonQuestion(CourseDto.LessonQuestionRequest req) {
        Lesson lesson = lessonRepo.findById(req.getLessonId())
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));
        validateOptions(req.getOptions(), req.getCorrectAnswers());
        LessonQuestion q = LessonQuestion.builder()
                .questionText(req.getQuestionText()).imageUrl(req.getImageUrl())
                .options(jsonUtil.toJson(req.getOptions()))
                .correctAnswers(jsonUtil.toJson(req.getCorrectAnswers()))
                .lesson(lesson).build();
        return mapQuestion(lessonQRepo.save(q));
    }

    @Transactional
    public void deleteLessonQuestion(Long id) {
        lessonQRepo.delete(lessonQRepo.findById(id).orElseThrow(() -> new NotFoundException("Savol topilmadi")));
    }

    @Transactional
    public CourseDto.QuestionResponse addSectionFinalQuestion(CourseDto.SectionFinalQuestionRequest req) {
        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));
        validateOptions(req.getOptions(), req.getCorrectAnswers());
        SectionFinalQuestion q = SectionFinalQuestion.builder()
                .questionText(req.getQuestionText()).imageUrl(req.getImageUrl())
                .options(jsonUtil.toJson(req.getOptions()))
                .correctAnswers(jsonUtil.toJson(req.getCorrectAnswers()))
                .section(section).build();
        return mapFinalQuestion(sectionFQRepo.save(q));
    }

    @Transactional
    public void deleteSectionFinalQuestion(Long id) {
        sectionFQRepo.delete(sectionFQRepo.findById(id).orElseThrow(() -> new NotFoundException("Savol topilmadi")));
    }

    // ==========================================================
    // SAVOLLAR — JSON bulk import
    // ==========================================================

    @Transactional
    public int bulkImportLessonQuestions(CourseDto.BulkImportRequest req) {
        if (req.getLessonId() == null) throw new BadRequestException("lessonId kiritish shart");
        Lesson lesson = lessonRepo.findById(req.getLessonId())
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));
        int count = 0;
        for (CourseDto.QuestionImportItem item : req.getQuestions()) {
            NormalizedQuestion nq = normalizeQuestion(item);
            validateOptions(nq.options(), nq.correctAnswers());
            lessonQRepo.save(LessonQuestion.builder()
                    .questionText(nq.questionText())
                    .imageUrl(nq.imageUrl())
                    .options(jsonUtil.toJson(nq.options()))
                    .correctAnswers(jsonUtil.toJson(nq.correctAnswers()))
                    .lesson(lesson).build());
            count++;
        }
        log.info("Bulk import: {} savol darsga qo'shildi (lessonId={})", count, lesson.getId());
        return count;
    }

    @Transactional
    public int bulkImportFinalQuestions(CourseDto.BulkImportRequest req) {
        if (req.getSectionId() == null) throw new BadRequestException("sectionId kiritish shart");
        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));
        int count = 0;
        for (CourseDto.QuestionImportItem item : req.getQuestions()) {
            NormalizedQuestion nq = normalizeQuestion(item);
            validateOptions(nq.options(), nq.correctAnswers());
            sectionFQRepo.save(SectionFinalQuestion.builder()
                    .questionText(nq.questionText())
                    .imageUrl(nq.imageUrl())
                    .options(jsonUtil.toJson(nq.options()))
                    .correctAnswers(jsonUtil.toJson(nq.correctAnswers()))
                    .section(section).build());
            count++;
        }
        log.info("Bulk import: {} final savol bo'limga qo'shildi (sectionId={})", count, section.getId());
        return count;
    }

    /**
     * Ikkala JSON formatini bir xil ichki strukturaga keltiradi.
     *
     * Format 1 (oddiy):
     *   { "questionText": "...", "imageUrl": "...", "options": [...], "correctAnswers": [...] }
     *
     * Format 2 (JSON fayl — Eng_oxirgi_61_4.json strukturasi):
     *   {
     *     "id": "1.1", "topic_id": "1.9",
     *     "question_text": "...", "image": "...",
     *     "answers": [ { "option": "A", "text": "...", "gif": "" }, ... ],
     *     "correct_answer": "To'g'ri javob matni"
     *   }
     */
    private NormalizedQuestion normalizeQuestion(CourseDto.QuestionImportItem item) {
        // Format 2: answers[] ro'yxati mavjud bo'lsa
        if (item.getAnswers() != null && !item.getAnswers().isEmpty()) {
            String questionText = item.getQuestion_text();
            if (questionText == null || questionText.isBlank())
                throw new BadRequestException("question_text bo'sh bo'lmasligi kerak");

            List<String> options = item.getAnswers().stream()
                    .map(CourseDto.AnswerOption::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .toList();

            String correctAnswerText = item.getCorrect_answer();
            if (correctAnswerText == null || correctAnswerText.isBlank())
                throw new BadRequestException("correct_answer bo'sh bo'lmasligi kerak");

            List<String> correctAnswers = List.of(correctAnswerText);

            String imageUrl = (item.getImage() != null && !item.getImage().isBlank())
                    ? item.getImage() : null;

            return new NormalizedQuestion(questionText, imageUrl, options, correctAnswers);
        }

        // Format 1: oddiy format
        String questionText = item.getQuestionText();
        if (questionText == null || questionText.isBlank())
            throw new BadRequestException("questionText bo'sh bo'lmasligi kerak");

        return new NormalizedQuestion(
                questionText,
                item.getImageUrl(),
                item.getOptions(),
                item.getCorrectAnswers()
        );
    }

    /** Normalize natijasi uchun ichki yordamchi record */
    private record NormalizedQuestion(
            String questionText,
            String imageUrl,
            List<String> options,
            List<String> correctAnswers
    ) {}

    // ==========================================================
    // SAVOLLARNI OLISH (foydalanuvchi uchun)
    // ==========================================================

    public List<CourseDto.QuestionResponse> getLessonQuestions(Long lessonId, Long userId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));
        if (userId != null) {
            boolean watched = lessonProgressRepo.findByUserIdAndLessonId(userId, lessonId)
                    .map(UserLessonProgress::isVideoWatched).orElse(false);
            if (!watched) throw new BadRequestException("Avval videoni tomosha qiling");
        }
        return lessonQRepo.findByLessonId(lessonId).stream().map(this::mapQuestion).toList();
    }

    public List<CourseDto.QuestionResponse> getSectionFinalQuestions(Long sectionId, Long userId) {
        sectionRepo.findById(sectionId).orElseThrow(() -> new NotFoundException("Bo'lim topilmadi"));
        if (userId != null) {
            boolean allPassed = lessonProgressRepo.areAllLessonsPassedInSection(userId, sectionId);
            if (!allPassed) {
                long passed = lessonProgressRepo.countPassedLessonsInSection(userId, sectionId);
                long total  = lessonRepo.countBySectionId(sectionId);
                throw new BadRequestException("Barcha darslar testini toping: " + passed + "/" + total);
            }
        }
        return sectionFQRepo.findBySectionId(sectionId).stream().map(this::mapFinalQuestion).toList();
    }

    // ==========================================================
    // MAPPER
    // ==========================================================

    private CourseDto.CourseResponse mapCourse(Course course, Long userId, User user) {
        List<Section> sections = sectionRepo.findByCourseIdOrderByOrderIndexAsc(course.getId());
        int totalLessons = sections.stream().mapToInt(s -> (int) lessonRepo.countBySectionId(s.getId())).sum();
        return CourseDto.CourseResponse.builder()
                .id(course.getId()).title(course.getTitle()).description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl()).totalSections(sections.size())
                .totalLessons(totalLessons).createdAt(course.getCreatedAt())
                .sections(sections.stream().map(s -> mapSection(s, userId, user)).toList())
                .build();
    }

    private CourseDto.SectionResponse mapSection(Section section, Long userId, User user) {
        List<Lesson> lessons = lessonRepo.findBySectionIdOrderByOrderIndexAsc(section.getId());

        // ── Bo'lim ochiq/yopiqligini aniqlash ──────────────────
        // 1-bo'lim: har doim ochiq (login kerak emas)
        // 2+ bo'lim: oldingi bo'lim yakuniy testida >= 80% ball kerak
        boolean unlocked = section.getOrderIndex() == 1
                || (userId != null && isSectionUnlocked(userId, section));

        int passed = 0;
        boolean finalAvail = false, finalPassed = false;
        Integer finalBest = null;
        int finalAttempts = 0;

        if (userId != null && unlocked) {
            passed     = (int) lessonProgressRepo.countPassedLessonsInSection(userId, section.getId());
            finalAvail = lessonProgressRepo.areAllLessonsPassedInSection(userId, section.getId());

            Optional<UserSectionProgress> sp =
                    sectionProgressRepo.findByUserIdAndSectionId(userId, section.getId());
            if (sp.isPresent()) {
                finalPassed   = sp.get().isFinalTestPassed();
                finalBest     = sp.get().getBestScore();
                finalAttempts = sp.get().getFinalTestAttempts();
            }
        }

        int finalTestQCount = (int) sectionFQRepo.countBySectionId(section.getId());

        return CourseDto.SectionResponse.builder()
                .id(section.getId()).title(section.getTitle()).description(section.getDescription())
                .orderIndex(section.getOrderIndex()).totalLessons(lessons.size())
                .passedLessons(passed).isUnlocked(unlocked)
                .finalTestAvailable(finalAvail).finalTestPassed(finalPassed)
                .finalTestBestScore(finalBest).finalTestAttempts(finalAttempts)
                .finalTestQuestions(finalTestQCount)
                .lessons(lessons.stream().map(l -> mapLesson(l, userId, user)).toList())
                .build();
    }

    public CourseDto.LessonResponse mapLesson(Lesson lesson, Long userId, User user) {
        boolean isFirstLesson = lesson.getOrderIndex() == 1 &&
                sectionRepo.findById(lesson.getSection().getId())
                        .map(s -> s.getOrderIndex() == 1).orElse(false);

        boolean canWatch      = false;
        boolean premiumLocked = false;
        boolean videoWatched  = false;
        boolean testPassed    = false;
        Integer bestScore     = null;
        int     attempts      = 0;

        if (userId == null) {
            // Login qilinmagan — faqat 1-bo'lim 1-darsini ko'ra oladi
            canWatch = isFirstLesson;
        } else {
            boolean hasPremium  = user != null && user.hasActivePremium();
            // Bo'lim 80% shartiga qarab ochiq/yopiq
            boolean sectionOpen = lesson.getSection().getOrderIndex() == 1
                    || isSectionUnlocked(userId, lesson.getSection());

            if (!sectionOpen) {
                // Bo'lim qulflangan — darsga kirish yo'q
                canWatch = false;
            } else if (isFirstLesson) {
                // 1-bo'lim 1-darsi — hamma ko'ra oladi (hatto premium yo'q bo'lsa ham)
                canWatch = true;
            } else if (!hasPremium) {
                // Premium yo'q — faqat birinchi dars ochiq, qolganlari yopiq
                canWatch      = false;
                premiumLocked = true;
            } else {
                // Premium bor, bo'lim ochiq — dars tartibiga qarab tekshir
                canWatch = canWatchLesson(userId, lesson);
            }

            Optional<UserLessonProgress> prog =
                    lessonProgressRepo.findByUserIdAndLessonId(userId, lesson.getId());
            if (prog.isPresent()) {
                videoWatched = prog.get().isVideoWatched();
                testPassed   = prog.get().isTestPassed();
                bestScore    = prog.get().getBestScore();
                attempts     = prog.get().getTestAttempts();
            }
        }

        // VideoUrls list qilish
        List<String> videoUrlsList = null;
        if (canWatch) {
            if (lesson.getVideoUrls() != null && !lesson.getVideoUrls().isBlank()) {
                try { videoUrlsList = jsonUtil.fromJson(lesson.getVideoUrls()); } catch (Exception ignored) {}
            }
            if (videoUrlsList == null || videoUrlsList.isEmpty()) {
                videoUrlsList = lesson.getVideoUrl() != null ? List.of(lesson.getVideoUrl()) : null;
            }
        }

        return CourseDto.LessonResponse.builder()
                .id(lesson.getId()).title(lesson.getTitle()).description(lesson.getDescription())
                .videoUrl(canWatch ? lesson.getVideoUrl() : null)
                .videoUrls(videoUrlsList)
                .durationMinutes(lesson.getDurationMinutes()).orderIndex(lesson.getOrderIndex())
                .isFree(lesson.isFree()).isLocked(!canWatch).isPremiumLocked(premiumLocked)
                .videoWatched(videoWatched).testPassed(testPassed)
                .bestScore(bestScore).testAttempts(attempts)
                .totalQuestions((int) lessonQRepo.countByLessonId(lesson.getId()))
                .build();
    }

    public CourseDto.QuestionResponse mapQuestion(LessonQuestion q) {
        return CourseDto.QuestionResponse.builder()
                .id(q.getId()).questionText(q.getQuestionText()).imageUrl(q.getImageUrl())
                .options(jsonUtil.fromJson(q.getOptions())).build();
    }

    public CourseDto.QuestionResponse mapFinalQuestion(SectionFinalQuestion q) {
        return CourseDto.QuestionResponse.builder()
                .id(q.getId()).questionText(q.getQuestionText()).imageUrl(q.getImageUrl())
                .options(jsonUtil.fromJson(q.getOptions())).build();
    }

    // ==========================================================
    // YORDAMCHI METODLAR
    // ==========================================================

    /**
     * Bo'limni ochish sharti:
     *   - 1-bo'lim — har doim ochiq
     *   - N-bo'lim — oldingi bo'lim yakuniy testida bestScore >= 80% bo'lsa ochiq
     *
     * Eslatma: avval faqat finalTestPassed=true tekshirilgan edi,
     * lekin ball 80% dan past bo'lib, o'tilgan holat ham bo'lishi mumkin.
     * Shuning uchun bestScore >= SECTION_UNLOCK_MIN_SCORE orqali tekshiramiz.
     */
    public boolean isSectionUnlocked(Long userId, Section section) {
        if (section.getOrderIndex() == 1) return true;

        Optional<Section> prev = sectionRepo.findByCourseIdAndOrderIndex(
                section.getCourse().getId(), section.getOrderIndex() - 1);
        if (prev.isEmpty()) return true;   // oldingi bo'lim yo'q — ochiq deb hisoblaymiz

        return sectionProgressRepo
                .findByUserIdAndSectionId(userId, prev.get().getId())
                .map(sp -> sp.getBestScore() != null
                        && sp.getBestScore() >= SECTION_UNLOCK_MIN_SCORE)
                .orElse(false);
    }

    public boolean canWatchLesson(Long userId, Lesson lesson) {
        if (!isSectionUnlocked(userId, lesson.getSection())) return false;
        if (lesson.getOrderIndex() == 1) return true;

        Optional<Lesson> prev = lessonRepo.findBySectionIdAndOrderIndex(
                lesson.getSection().getId(), lesson.getOrderIndex() - 1);
        if (prev.isEmpty()) return true;

        return lessonProgressRepo.findByUserIdAndLessonId(userId, prev.get().getId())
                .map(UserLessonProgress::isTestPassed).orElse(false);
    }

    private Course getActiveCourse() {
        return courseRepo.findFirstActiveCourse()
                .orElseThrow(() -> new NotFoundException(
                        "Kurs topilmadi. Avval admin paneldan kurs yarating."));
    }

    private void validateOptions(List<String> options, List<String> correctAnswers) {
        if (options == null || options.size() < 2)
            throw new BadRequestException("Kamida 2 ta variant bo'lishi kerak");
        if (correctAnswers == null || correctAnswers.isEmpty())
            throw new BadRequestException("Kamida 1 ta to'g'ri javob bo'lishi kerak");
        for (String ca : correctAnswers) {
            boolean found = options.stream().anyMatch(o -> o.equalsIgnoreCase(ca));
            if (!found) throw new BadRequestException("\"" + ca + "\" variantlar ichida yo'q");
        }
    }

    public String toEmbedUrl(String url) {
        if (url == null) return null;
        if (url.contains("youtube.com/embed/") || url.contains("player.vimeo.com")) return url;
        if (url.contains("youtu.be/")) {
            String id = url.split("youtu.be/")[1].split("[?&]")[0];
            return "https://www.youtube.com/embed/" + id;
        }
        if (url.contains("youtube.com/watch")) {
            for (String p : url.split("[?&]"))
                if (p.startsWith("v=")) return "https://www.youtube.com/embed/" + p.substring(2);
        }
        return url;
    }
}