package tz.go.tirdo.teltp.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.content.entity.LearningMaterial;

import java.util.List;
import java.util.Optional;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
    Optional<LearningMaterial> findByUuid(String uuid);
    List<LearningMaterial> findByLessonUuid(String lessonUuid);
}
