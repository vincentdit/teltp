package tz.go.tirdo.teltp.enrollment.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.catalog.entity.Course;
import tz.go.tirdo.teltp.catalog.entity.CourseStatus;
import tz.go.tirdo.teltp.catalog.service.CourseService;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.enrollment.dto.EnrollmentDtos.*;
import tz.go.tirdo.teltp.enrollment.entity.Cohort;
import tz.go.tirdo.teltp.enrollment.entity.Enrollment;
import tz.go.tirdo.teltp.enrollment.entity.EnrollmentStatus;
import tz.go.tirdo.teltp.enrollment.repository.CohortRepository;
import tz.go.tirdo.teltp.enrollment.repository.EnrollmentRepository;

import java.util.HashSet;
import java.util.Set;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final CohortRepository cohorts;
    private final CourseService courseService;

    public EnrollmentService(EnrollmentRepository enrollments, CohortRepository cohorts, CourseService courseService) {
        this.enrollments = enrollments;
        this.cohorts = cohorts;
        this.courseService = courseService;
    }

    @Transactional
    public CohortResponse createCohort(CohortRequest req) {
        courseService.getEntity(req.courseUuid()); // validates course exists
        Cohort c = new Cohort();
        c.setCourseUuid(req.courseUuid());
        c.setName(req.name());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        c.setCapacity(req.capacity());
        return toCohortResponse(cohorts.save(c));
    }

    /** Self-enrolment by the authenticated student. Free courses go ACTIVE; paid go PENDING_PAYMENT. */
    @Transactional
    public EnrollmentResponse selfEnroll(String studentUuid, EnrollRequest req) {
        Course course = courseService.getEntity(req.courseUuid());
        if (course.getStatus() != CourseStatus.PUBLISHED)
            throw new BusinessRuleException("Course is not open for enrolment");

        EnrollmentStatus initial = resolveInitialStatus(course, req.cohortUuid());
        Enrollment e = buildEnrollment(req.courseUuid(), studentUuid, req.cohortUuid(), null, initial);
        return toResponse(enrollments.save(e));
    }

    /** Admin/org-admin bulk assignment for a corporate cohort. */
    @Transactional
    public Set<EnrollmentResponse> adminAssign(AdminAssignRequest req) {
        courseService.getEntity(req.courseUuid());
        Set<EnrollmentResponse> created = new HashSet<>();
        for (String studentUuid : req.studentUuids()) {
            Enrollment e = buildEnrollment(req.courseUuid(), studentUuid, req.cohortUuid(),
                    req.organizationUuid(), EnrollmentStatus.ACTIVE);
            created.add(toResponse(enrollments.save(e)));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> myEnrollments(String studentUuid, Pageable pageable) {
        return PageResponse.from(enrollments.findByStudentUuid(studentUuid, pageable), this::toResponse);
    }

    /** Cross-module hook for Progress/Certification to confirm an active enrolment. */
    public Enrollment getEntity(String uuid) {
        return enrollments.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", uuid));
    }

    @Transactional
    public void markCompleted(String enrollmentUuid) {
        Enrollment e = getEntity(enrollmentUuid);
        e.setStatus(EnrollmentStatus.COMPLETED);
        e.setCompletedDate(java.time.Instant.now());
        enrollments.save(e);
    }

    private EnrollmentStatus resolveInitialStatus(Course course, String cohortUuid) {
        if (cohortUuid != null) {
            Cohort cohort = cohorts.findByUuid(cohortUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Cohort", cohortUuid));
            if (cohort.getCapacity() != null) {
                long active = enrollments.countByCohortUuidAndStatus(cohortUuid, EnrollmentStatus.ACTIVE);
                if (active >= cohort.getCapacity()) return EnrollmentStatus.WAITLISTED;
            }
        }
        boolean paid = course.getPricingPlanUuid() != null;
        return paid ? EnrollmentStatus.PENDING_PAYMENT : EnrollmentStatus.ACTIVE;
    }

    private Enrollment buildEnrollment(String courseUuid, String studentUuid, String cohortUuid,
                                       String orgUuid, EnrollmentStatus status) {
        enrollments.findByCourseUuidAndStudentUuidAndCohortUuid(courseUuid, studentUuid, cohortUuid)
                .ifPresent(x -> { throw new BusinessRuleException("Already enrolled in this course/cohort"); });
        Enrollment e = new Enrollment();
        e.setCourseUuid(courseUuid);
        e.setStudentUuid(studentUuid);
        e.setCohortUuid(cohortUuid);
        e.setAssignedByOrganizationUuid(orgUuid);
        e.setStatus(status);
        return e;
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getUuid(), e.getCourseUuid(), e.getStudentUuid(),
                e.getCohortUuid(), e.getStatus().name(), e.getAssignedByOrganizationUuid());
    }

    private CohortResponse toCohortResponse(Cohort c) {
        long active = enrollments.countByCohortUuidAndStatus(c.getUuid(), EnrollmentStatus.ACTIVE);
        return new CohortResponse(c.getUuid(), c.getCourseUuid(), c.getName(),
                c.getStartDate(), c.getEndDate(), c.getCapacity(), active);
    }
}
