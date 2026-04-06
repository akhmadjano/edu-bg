package com.platform.repository;

import com.platform.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {

    Optional<UserLessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<UserLessonProgress> findByUserIdAndLesson_Section_Id(Long userId, Long sectionId);

    @Query("""
        SELECT COUNT(l) = COUNT(ulp)
        FROM Lesson l
        LEFT JOIN UserLessonProgress ulp
            ON ulp.lesson.id = l.id
            AND ulp.user.id = :userId
            AND ulp.testPassed = true
        WHERE l.section.id = :sectionId
    """)
    boolean areAllLessonsPassedInSection(@Param("userId") Long userId,
                                         @Param("sectionId") Long sectionId);

    @Query("""
        SELECT COUNT(ulp)
        FROM UserLessonProgress ulp
        WHERE ulp.user.id = :userId
          AND ulp.lesson.section.id = :sectionId
          AND ulp.testPassed = true
    """)
    long countPassedLessonsInSection(@Param("userId") Long userId,
                                     @Param("sectionId") Long sectionId);
}