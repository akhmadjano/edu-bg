package com.platform.repository;

import com.platform.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.id ASC")
    List<Course> findActiveCourses();

    // Eski CourseService uchun (hali yangi fayl o'rnatilmagan bo'lsa ishlaydi)
    default Optional<Course> findFirstByIsActiveTrueOrderByIdAsc() {
        return findFirstActiveCourse();
    }

    // Yangi CourseService uchun
    default Optional<Course> findFirstActiveCourse() {
        List<Course> list = findActiveCourses();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}