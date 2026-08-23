package tz.go.tirdo.teltp.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.certification.entity.Certificate;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByUuid(String uuid);
    Optional<Certificate> findByVerificationCode(String verificationCode);
    Optional<Certificate> findByStudentUuidAndCourseUuid(String studentUuid, String courseUuid);
    List<Certificate> findByStudentUuid(String studentUuid);
    long countByCourseUuid(String courseUuid);
}
