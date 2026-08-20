package tz.go.tirdo.teltp.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.catalog.entity.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Optional<Lesson> findByUuid(String uuid);
    List<Lesson> findByModuleCourseIdAndMandatoryTrue(Long courseId);
    long countByModuleCourseIdAndMandatoryTrue(Long courseId);
    long countByModuleCourseUuidAndMandatoryTrue(String courseUuid);
}
