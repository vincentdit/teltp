package tz.go.tirdo.teltp.auth.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.service.AuthService;
import tz.go.tirdo.teltp.common.ApiResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("Registration successful", auth.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(auth.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(auth.refresh(req));
    }
}
