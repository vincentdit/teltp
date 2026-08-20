package tz.go.tirdo.teltp.progress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.progress.entity.LessonProgress;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByStudentUuidAndLessonUuid(String studentUuid, String lessonUuid);
    List<LessonProgress> findByStudentUuidAndCourseUuid(String studentUuid, String courseUuid);
    long countByStudentUuidAndCourseUuidAndCompletedTrue(String studentUuid, String courseUuid);
}
