package com.platform.repository;

import com.platform.entity.LessonQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonQuestionRepository extends JpaRepository<LessonQuestion, Long> {
    List<LessonQuestion> findByLessonId(Long lessonId);
    int countByLessonId(Long lessonId);
}