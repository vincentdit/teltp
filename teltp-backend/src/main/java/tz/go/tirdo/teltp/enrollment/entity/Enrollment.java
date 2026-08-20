package tz.go.tirdo.teltp.enrollment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_uuid", "student_uuid", "cohort_uuid"}))
public class Enrollment extends BaseEntity {

    @Column(name = "course_uuid", nullable = false, length = 36)
    private String courseUuid;

    @Column(name = "student_uuid", nullable = false, length = 36)
    private String studentUuid;

    @Column(name = "cohort_uuid", length = 36)
    private String cohortUuid;

    /** Set when an organization admin assigns the enrollment for a corporate cohort. */
    @Column(length = 36)
    private String assignedByOrganizationUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(nullable = false)
    private Instant enrollmentDate = Instant.now();

    private Instant completedDate;
}
