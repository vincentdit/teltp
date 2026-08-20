package tz.go.tirdo.teltp.enrollment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.enrollment.entity.Cohort;

import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByUuid(String uuid);
}
