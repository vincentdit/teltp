package tz.go.tirdo.teltp.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.catalog.entity.Course;
import tz.go.tirdo.teltp.catalog.entity.CourseStatus;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByUuid(String uuid);
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
    Page<Course> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByReferenceNumber(String referenceNumber);
}
