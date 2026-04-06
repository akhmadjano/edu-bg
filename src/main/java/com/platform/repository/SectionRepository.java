package com.platform.repository;

import com.platform.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    Optional<Section> findByCourseIdAndOrderIndex(Long courseId, Integer orderIndex);
}