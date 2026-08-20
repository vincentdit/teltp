package tz.go.tirdo.teltp.notification.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.notification.dto.NotificationDtos.*;
import tz.go.tirdo.teltp.notification.service.NotificationService;
import tz.go.tirdo.teltp.security.CurrentUser;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final UserService users;

    public NotificationController(NotificationService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<NotificationResponse>> inbox(Pageable pageable) {
        return ApiResponse.ok(service.inbox(me(), pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> unread() {
        return ApiResponse.ok(service.unreadCount(me()));
    }

    @PostMapping("/{uuid}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markRead(@PathVariable String uuid) {
        service.markRead(uuid);
        return ApiResponse.ok("Marked read", null);
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
