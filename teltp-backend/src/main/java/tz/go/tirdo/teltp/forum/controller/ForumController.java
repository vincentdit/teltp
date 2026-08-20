package tz.go.tirdo.teltp.forum.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.forum.dto.ForumDtos.*;
import tz.go.tirdo.teltp.forum.service.ForumService;
import tz.go.tirdo.teltp.security.CurrentUser;

@RestController
@RequestMapping("/forum")
public class ForumController {

    private final ForumService service;
    private final UserService users;

    public ForumController(ForumService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ThreadResponse> createThread(@Valid @RequestBody CreateThreadRequest req) {
        return ApiResponse.ok(service.createThread(me(), req));
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PostResponse> reply(@Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(service.reply(me(), req));
    }

    @GetMapping("/courses/{courseUuid}/threads")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<ThreadResponse>> threads(@PathVariable String courseUuid, Pageable pageable) {
        return ApiResponse.ok(service.threadsForCourse(courseUuid, pageable));
    }

    @GetMapping("/threads/{threadUuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ThreadDetailResponse> detail(@PathVariable String threadUuid) {
        return ApiResponse.ok(service.threadDetail(threadUuid));
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
