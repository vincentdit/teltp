package tz.go.tirdo.teltp.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.assessment.entity.Question;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByUuid(String uuid);
}
