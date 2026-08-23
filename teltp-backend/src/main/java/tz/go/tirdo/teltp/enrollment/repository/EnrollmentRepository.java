package tz.go.tirdo.teltp.enrollment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.enrollment.entity.Enrollment;
import tz.go.tirdo.teltp.enrollment.entity.EnrollmentStatus;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByUuid(String uuid);
    Optional<Enrollment> findByCourseUuidAndStudentUuidAndCohortUuid(String courseUuid, String studentUuid, String cohortUuid);
    Page<Enrollment> findByStudentUuid(String studentUuid, Pageable pageable);
    List<Enrollment> findByCourseUuidAndStatus(String courseUuid, EnrollmentStatus status);
    long countByCohortUuidAndStatus(String cohortUuid, EnrollmentStatus status);
    long countByCourseUuid(String courseUuid);
}
