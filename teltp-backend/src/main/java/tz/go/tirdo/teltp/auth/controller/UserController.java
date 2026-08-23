package tz.go.tirdo.teltp.auth.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.service.AuthService;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

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
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ApiResponse.ok(users.search(q, pageable));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(users.get(uuid));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> assignRoles(@Valid @RequestBody AssignRolesRequest req,
                                                 Authentication authentication) {
        // An admin must not strip ADMIN from their own account (self-lockout).
        if (isSelf(req.userUuid(), authentication)
                && (req.roles() == null || !req.roles().contains("ADMIN"))) {
            throw new BusinessRuleException("You cannot remove your own ADMIN role.");
        }
        return ApiResponse.ok("Roles updated", auth.assignRoles(req));
    }

    @PatchMapping("/{uuid}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> setActive(@PathVariable String uuid, @RequestParam boolean active,
                                               Authentication authentication) {
        // An admin must not deactivate their own account (self-lockout).
        if (!active && isSelf(uuid, authentication)) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }
        return ApiResponse.ok(users.setActive(uuid, active));
    }

    @PostMapping("/{uuid}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> resetPassword(@PathVariable String uuid,
                                                    @Valid @RequestBody ResetPasswordRequest req) {
        return ApiResponse.ok("Password reset", auth.resetPassword(uuid, req.newPassword()));
    }

    /** True when the given user uuid is the currently authenticated user. */
    private boolean isSelf(String targetUuid, Authentication authentication) {
        if (authentication == null || targetUuid == null) return false;
        String myUuid = users.uuidForUsername(authentication.getName());
        return myUuid != null && myUuid.equals(targetUuid);
    }
}
