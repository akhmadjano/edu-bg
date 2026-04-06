package com.platform.repository;

import com.platform.entity.SectionFinalQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionFinalQuestionRepository extends JpaRepository<SectionFinalQuestion, Long> {
    List<SectionFinalQuestion> findBySectionId(Long sectionId);
    int countBySectionId(Long sectionId);
}