package com.platform.repository;

import com.platform.entity.UserSectionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSectionProgressRepository extends JpaRepository<UserSectionProgress, Long> {
    Optional<UserSectionProgress> findByUserIdAndSectionId(Long userId, Long sectionId);
    boolean existsByUserIdAndSectionIdAndFinalTestPassedTrue(Long userId, Long sectionId);
    List<UserSectionProgress> findByUserId(Long userId);
}