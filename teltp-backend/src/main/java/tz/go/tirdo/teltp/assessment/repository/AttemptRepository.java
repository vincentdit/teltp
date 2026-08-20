package tz.go.tirdo.teltp.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.assessment.entity.Attempt;
import tz.go.tirdo.teltp.assessment.entity.AttemptStatus;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    Optional<Attempt> findByUuid(String uuid);
    List<Attempt> findByStudentUuidAndAssessmentUuid(String studentUuid, String assessmentUuid);
    List<Attempt> findByStatus(AttemptStatus status);
}
