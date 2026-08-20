package tz.go.tirdo.teltp.progress.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.progress.dto.ProgressDtos.*;
import tz.go.tirdo.teltp.progress.service.ProgressService;

import java.util.List;
import tz.go.tirdo.teltp.security.CurrentUser;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService service;
    private final UserService users;

    public ProgressController(ProgressService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CourseProgressResponse> mark(@Valid @RequestBody MarkLessonRequest req) {
        return ApiResponse.ok(service.mark(me(), req));
    }

    @GetMapping("/courses/{courseUuid}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CourseProgressResponse> course(@PathVariable String courseUuid) {
        return ApiResponse.ok(service.computeForCourse(me(), courseUuid));
    }

    @GetMapping("/courses/{courseUuid}/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<LessonProgressView>> lessons(@PathVariable String courseUuid) {
        return ApiResponse.ok(service.lessonProgress(me(), courseUuid));
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
