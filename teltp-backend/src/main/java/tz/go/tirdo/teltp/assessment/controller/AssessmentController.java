package tz.go.tirdo.teltp.assessment.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.assessment.dto.AssessmentDtos.*;
import tz.go.tirdo.teltp.assessment.service.AssessmentService;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentService service;
    private final UserService users;

    public AssessmentController(AssessmentService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<AssessmentResponse> create(@Valid @RequestBody CreateAssessmentRequest req) {
        return ApiResponse.ok("Assessment created", service.create(req));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<QuestionView> addQuestion(@Valid @RequestBody AddQuestionRequest req) {
        return ApiResponse.ok(service.addQuestion(req));
    }

    @GetMapping("/courses/{courseUuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AssessmentResponse>> forCourse(@PathVariable String courseUuid) {
        return ApiResponse.ok(service.listForCourse(courseUuid));
    }

    @GetMapping("/{uuid}/view")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AssessmentView> view(@PathVariable String uuid) {
        return ApiResponse.ok(service.view(uuid));
    }

    @PostMapping("/{uuid}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttemptResponse> start(@PathVariable String uuid) {
        return ApiResponse.ok(service.startAttempt(me(), uuid));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttemptResponse> submit(@Valid @RequestBody SubmitAttemptRequest req) {
        return ApiResponse.ok("Submitted", service.submit(me(), req));
    }

    @PostMapping("/grade")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<AttemptResponse> grade(@Valid @RequestBody GradeAnswerRequest req) {
        return ApiResponse.ok("Graded", service.gradeAnswer(req));
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
