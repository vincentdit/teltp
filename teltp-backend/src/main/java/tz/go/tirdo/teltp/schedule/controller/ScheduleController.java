package tz.go.tirdo.teltp.schedule.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.schedule.dto.ScheduleDtos.*;
import tz.go.tirdo.teltp.schedule.service.ScheduleService;
import tz.go.tirdo.teltp.security.CurrentUser;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService service;
    private final UserService users;

    public ScheduleController(ScheduleService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest req) {
        return ApiResponse.ok("Event created", service.create(req));
    }

    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<EventResponse>> list(@RequestParam(defaultValue = "WEBINAR") String type,
                                                         Pageable pageable) {
        return ApiResponse.ok(service.listByType(type, pageable));
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("Registered", service.register(me(), req));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<AttendanceResponse> markAttendance(@Valid @RequestBody MarkAttendanceRequest req) {
        return ApiResponse.ok(service.markAttendance(req));
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
