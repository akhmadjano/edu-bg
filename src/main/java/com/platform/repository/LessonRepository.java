package com.platform.repository;

import com.platform.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    Optional<Lesson> findBySectionIdAndOrderIndex(Long sectionId, Integer orderIndex);
    int countBySectionId(Long sectionId);
}