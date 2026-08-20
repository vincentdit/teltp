package tz.go.tirdo.teltp.enrollment.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.enrollment.dto.EnrollmentDtos.*;
import tz.go.tirdo.teltp.enrollment.service.EnrollmentService;
import tz.go.tirdo.teltp.security.CurrentUser;

import java.util.Set;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;
    private final UserService users;

    public EnrollmentController(EnrollmentService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/cohorts")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<CohortResponse> createCohort(@Valid @RequestBody CohortRequest req) {
        return ApiResponse.ok(service.createCohort(req));
    }

    @PostMapping("/self")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<EnrollmentResponse> selfEnroll(@Valid @RequestBody EnrollRequest req) {
        return ApiResponse.ok("Enrolled", service.selfEnroll(currentStudentUuid(), req));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','CORPORATE_CLIENT')")
    public ApiResponse<Set<EnrollmentResponse>> adminAssign(@Valid @RequestBody AdminAssignRequest req) {
        return ApiResponse.ok("Assigned", service.adminAssign(req));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<EnrollmentResponse>> mine(Pageable pageable) {
        return ApiResponse.ok(service.myEnrollments(currentStudentUuid(), pageable));
    }

    private String currentStudentUuid() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
