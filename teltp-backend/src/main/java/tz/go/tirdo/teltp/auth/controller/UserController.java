package tz.go.tirdo.teltp.auth.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.service.AuthService;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;
    private final AuthService auth;

    public UserController(UserService users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> list(Pageable pageable) {
        return ApiResponse.ok(users.list(pageable));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(users.get(uuid));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> assignRoles(@Valid @RequestBody AssignRolesRequest req) {
        return ApiResponse.ok("Roles updated", auth.assignRoles(req));
    }

    @PatchMapping("/{uuid}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> setActive(@PathVariable String uuid, @RequestParam boolean active) {
        return ApiResponse.ok(users.setActive(uuid, active));
    }
}
