package tz.go.tirdo.teltp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Request/response records for authentication and user management. */
public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Size(max = 150) String username,
            @NotBlank @Size(max = 200) String password,
            boolean rememberMe) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 200) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            String phoneNumber,
            String profession,
            boolean dataProcessingConsent) {}

    public record RefreshRequest(String refreshToken) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record CompleteResetRequest(
            @NotBlank String token,
            @NotBlank @Size(max = 200) String newPassword) {}

    public record VerifyEmailRequest(@NotBlank String token) {}

    public record ResendVerificationRequest(@NotBlank @Email String email) {}

    public record MfaVerifyRequest(@NotBlank String challenge, @NotBlank String code, boolean rememberMe) {}

    public record MfaCodeRequest(@NotBlank String code) {}

    public record MfaSetupResponse(String secret, String otpauthUri) {}

    public record NidaSubmitRequest(@NotBlank @Size(max = 40) String nationalId) {}

    /**
     * When {@code mfaRequired} is true, the access and refresh tokens are empty and
     * {@code mfaChallenge} must be sent to /auth/mfa/verify with a TOTP code.
     * The refresh token is also set as an HttpOnly cookie and is omitted from JSON
     * by the controller.
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            boolean mfaRequired,
            String mfaChallenge) {}

    public record UserResponse(
            String uuid,
            String username,
            String email,
            String fullName,
            String profession,
            String organizationUuid,
            boolean active,
            Set<String> roles,
            boolean emailVerified) {}

    public record AssignRolesRequest(@NotBlank String userUuid, Set<String> roles) {}

    public record ResetPasswordRequest(@NotBlank @Size(max = 200) String newPassword) {}

    /** Tokens plus whether the refresh cookie should survive the browser being closed. */
    public record AuthSession(TokenResponse tokens, boolean persistent) {}
}
