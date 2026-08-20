package tz.go.tirdo.teltp.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.assessment.entity.Assessment;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByUuid(String uuid);
    List<Assessment> findByCourseUuid(String courseUuid);
}
