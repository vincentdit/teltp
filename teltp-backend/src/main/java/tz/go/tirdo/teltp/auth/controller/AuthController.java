package tz.go.tirdo.teltp.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.service.AuthService;
import tz.go.tirdo.teltp.auth.service.PasswordResetService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.security.CurrentUser;
import tz.go.tirdo.teltp.security.JwtService;
import tz.go.tirdo.teltp.security.RefreshCookie;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;
    private final PasswordResetService resets;
    private final JwtService jwt;

    public AuthController(AuthService auth, PasswordResetService resets, JwtService jwt) {
        this.auth = auth;
        this.resets = resets;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("Registration successful", auth.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                                            HttpServletResponse response) {
        return ApiResponse.ok(present(auth.login(req), request, response));
    }

    @PostMapping("/mfa/verify")
    public ApiResponse<TokenResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest req, HttpServletRequest request,
                                                HttpServletResponse response) {
        return ApiResponse.ok(present(auth.verifyMfa(req), request, response));
    }

    @PostMapping("/mfa/setup")
    public ApiResponse<MfaSetupResponse> beginMfa() {
        return ApiResponse.ok(auth.beginMfa(CurrentUser.requireUsername()));
    }

    @PostMapping("/mfa/confirm")
    public ApiResponse<Void> confirmMfa(@Valid @RequestBody MfaCodeRequest req) {
        auth.confirmMfa(CurrentUser.requireUsername(), req.code());
        return ApiResponse.ok("MFA enabled", null);
    }

    @PostMapping("/mfa/disable")
    public ApiResponse<Void> disableMfa(@Valid @RequestBody MfaCodeRequest req) {
        auth.disableMfa(CurrentUser.requireUsername(), req.code());
        return ApiResponse.ok("MFA disabled", null);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody(required = false) RefreshRequest body,
                                              HttpServletRequest request, HttpServletResponse response) {
        String raw = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken()
                : RefreshCookie.read(request);
        return ApiResponse.ok(present(auth.refresh(raw), request, response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        String raw = body != null && body.refreshToken() != null ? body.refreshToken() : RefreshCookie.read(request);
        auth.logout(raw);
        RefreshCookie.clear(response, request.isSecure());
        return ApiResponse.ok("Signed out", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        resets.request(req.email());
        return ApiResponse.ok("If that email is registered, a reset link has been sent.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody CompleteResetRequest req) {
        resets.complete(req.token(), req.newPassword());
        return ApiResponse.ok("Password updated. Sign in with the new password.", null);
    }

    /** Puts the refresh token in an HttpOnly cookie and strips it from the JSON body. */
    private TokenResponse present(TokenResponse tokens, HttpServletRequest request, HttpServletResponse response) {
        if (tokens.mfaRequired() || tokens.refreshToken() == null) return tokens;
        RefreshCookie.write(response, tokens.refreshToken(), jwt.refreshTtlSeconds(), request.isSecure());
        return new TokenResponse(tokens.accessToken(), null, tokens.tokenType(), tokens.expiresIn(), false, null);
    }
}
